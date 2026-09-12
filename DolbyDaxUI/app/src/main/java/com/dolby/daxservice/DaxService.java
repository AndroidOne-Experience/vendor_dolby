package com.dolby.daxservice;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IHwBinder;
import android.util.Log;
import com.dolby.dax.DolbyAudioEffect;
import java.util.ArrayList;
import vendor.dolby.hardware.dms.V2_0.IDms;
import vendor.dolby.hardware.dms.V2_0.IDmsCallbacks;

/** DAX backend hosted by the UI APK; the ROM supplies the native DMS implementation. */
public final class DaxService extends Service {
    private static final String TAG = "DaxService";
    public static final String PERMISSION = "com.dolby.permission.DOLBY_UPDATE_BROADCAST";
    private HandlerThread thread;
    private Handler handler;
    private AudioServerWatchDog watchdog;
    private DaxSettings settings;
    private IDms dms;
    private IDmsCallbacks callbacks;
    private IHwBinder.DeathRecipient deathRecipient;
    private DolbyAudioEffect effect;
    private int user;
    private volatile int generation;
    private volatile boolean restoring;
    private volatile boolean stopped;
    private boolean systemReceiverRegistered;
    private boolean reloadReceiverRegistered;
    private final Runnable connectTask = this::connect;

    private final BroadcastReceiver systemReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (stopped) return;
            String action = intent.getAction();
            if ("android.intent.action.USER_SWITCHED".equals(action)) {
                int nextUser = intent.getIntExtra("android.intent.extra.user_handle", -1);
                if (nextUser < 0 || nextUser == user) return;
                saveCurrent();
                generation++;
                user = nextUser;
                reconnect();
            } else if ("android.intent.action.USER_REMOVED".equals(action)) {
                int removed = intent.getIntExtra("android.intent.extra.user_handle", -1);
                if (removed >= 0 && removed != user) settings.remove(removed);
            } else if (Intent.ACTION_SHUTDOWN.equals(action)) {
                saveCurrent();
                stopped = true;
                cleanup();
            }
        }
    };

    private final BroadcastReceiver reloadReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (!stopped) reconnect();
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        thread = new HandlerThread("DolbyDaxBackend");
        thread.start();
        handler = new Handler(thread.getLooper());
        settings = new DaxSettings(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_SWITCHED");
        filter.addAction("android.intent.action.USER_REMOVED");
        filter.addAction(Intent.ACTION_SHUTDOWN);
        PlatformUsers.registerSystemReceiver(this, systemReceiver, filter, handler);
        systemReceiverRegistered = true;
        IntentFilter reloadFilter = new IntentFilter("com.dolby.intent.ACTION_RELOAD_TUNING");
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            registerReceiver(reloadReceiver, reloadFilter, PERMISSION, handler, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(reloadReceiver, reloadFilter, PERMISSION, handler);
        }
        reloadReceiverRegistered = true;
        handler.post(() -> {
            watchdog = new AudioServerWatchDog(handler, () -> {
                reconnect();
                PlatformUsers.sendToUser(this, new Intent("audio_server_restarted"), user);
            });
            watchdog.start();
            connect();
        });
    }

    private void connect() {
        if (stopped) return;
        handler.removeCallbacks(connectTask);
        disconnect();
        final int connection = ++generation;
        try {
            user = PlatformUsers.currentUser();
            dms = IDms.getService();
            if (dms == null) throw new IllegalStateException("DMS is not ready");
            deathRecipient = cookie -> handler.post(() -> {
                if (!stopped && connection == generation) reconnect();
            });
            if (!dms.linkToDeath(deathRecipient, connection)) {
                throw new IllegalStateException("Cannot watch DMS binder");
            }
            effect = new DolbyAudioEffect(-1, 0);
            // Restore with temporary control; the UI keeps its normal priority afterwards.
            restoring = true;
            DolbyAudioEffect control = null;
            try {
                control = new DolbyAudioEffect(1, 0);
                if (!control.hasControl()) throw new IllegalStateException("Cannot restore Dolby settings");
                // The OEM service only captures native defaults on first run. Replaying
                // every queried value while SWDAP is coming up can crash the vendor HAL.
                if (!settings.initialize(control, user)) settings.restore(control, user);
            } finally {
                if (control != null) control.release();
                restoring = false;
            }
            callbacks = new IDmsCallbacks.Stub() {
                @Override public void onDapParamUpdate(ArrayList<Byte> params) {
                    if (stopped || restoring || params == null || params.size() < 8) return;
                    byte[] data = new byte[params.size()];
                    for (int i = 0; i < data.length; i++) {
                        if (params.get(i) == null) return;
                        data[i] = params.get(i);
                    }
                    DapUpdate update = DapUpdate.decode(data);
                    if (update != null) handler.post(() -> {
                        if (!stopped && connection == generation) onUpdate(update);
                    });
                }
            };
            dms.registerClient(callbacks, 0, callbacks.hashCode());
            // Initial synchronization after restoration and after every reconnection.
            sendUpdate("profile_change", effect.getProfile());
            sendUpdate("ds_state_change", effect.getDsOn() ? 1 : 0);
            Log.i(TAG, "Dolby backend connected for user " + user);
        } catch (Exception | LinkageError e) {
            Log.w(TAG, "Dolby backend not ready; retrying in one second", e);
            disconnect();
            handler.postDelayed(connectTask, 1000);
        }
    }

    private void onUpdate(DapUpdate update) {
        if (effect == null) return;
        try {
            if (!"ds_state_change".equals(update.event)
                    && (update.value < 0 || update.value >= Math.min(4, effect.getNumOfProfiles()))) return;
            settings.save(effect, user, update);
            sendUpdate(update.event, update.value);
        } catch (RuntimeException e) {
            Log.w(TAG, "Cannot process Dolby parameter update", e);
            reconnect();
        }
    }

    private void sendUpdate(String event, int value) {
        PlatformUsers.sendToUser(this, new Intent(DapUpdate.ACTION)
                .putExtra(DapUpdate.EVENT, event).putExtra(DapUpdate.VALUE, value), user);
    }

    private void reconnect() {
        generation++;
        disconnect();
        handler.removeCallbacks(connectTask);
        if (!stopped) handler.postDelayed(connectTask, 1000);
    }

    private void saveCurrent() {
        if (effect == null) return;
        try { settings.saveAll(effect, user); }
        catch (RuntimeException e) { Log.w(TAG, "Cannot snapshot Dolby settings", e); }
    }

    private void disconnect() {
        if (dms != null) {
            try {
                if (callbacks != null) dms.unregisterClient(callbacks, 0, callbacks.hashCode());
            } catch (Exception e) { Log.d(TAG, "DMS callback already disconnected"); }
            try {
                if (deathRecipient != null) dms.unlinkToDeath(deathRecipient);
            } catch (Exception e) { Log.d(TAG, "DMS death recipient already disconnected"); }
        }
        callbacks = null;
        deathRecipient = null;
        dms = null;
        if (effect != null) {
            try { effect.release(); }
            catch (RuntimeException e) { Log.d(TAG, "Dolby effect already released"); }
            effect = null;
        }
    }

    private void cleanup() {
        generation++;
        if (watchdog != null) watchdog.stop();
        handler.removeCallbacksAndMessages(null);
        disconnect();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }
    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public void onDestroy() {
        stopped = true;
        if (systemReceiverRegistered) unregisterReceiver(systemReceiver);
        if (reloadReceiverRegistered) unregisterReceiver(reloadReceiver);
        handler.post(() -> {
            saveCurrent();
            cleanup();
            thread.quitSafely();
        });
        super.onDestroy();
    }
}
