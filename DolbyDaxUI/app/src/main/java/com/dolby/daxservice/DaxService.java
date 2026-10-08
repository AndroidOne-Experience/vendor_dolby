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
import com.dolby.dax.SpatialAudioProfile;
import android.media.Spatializer;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
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
    private Spatializer spatializer;
    private AudioManager deviceAudioManager;
    private boolean bluetoothAudioConnected;
    private final AudioDeviceCallback audioDeviceCallback = new AudioDeviceCallback() {
        @Override public void onAudioDevicesAdded(AudioDeviceInfo[] devices) {
            onAudioDevicesChanged();
        }
        @Override public void onAudioDevicesRemoved(AudioDeviceInfo[] devices) {
            onAudioDevicesChanged();
        }
    };
    private final Spatializer.OnSpatializerStateChangedListener spatialListener =
            new Spatializer.OnSpatializerStateChangedListener() {
        @Override public void onSpatializerEnabledChanged(Spatializer source, boolean enabled) {
            if (source != spatializer) return;
            handler.removeCallbacks(spatialDisabledTask);
            if (enabled) syncSpatialProfile();
            // Let output-device removal settle before deciding this was a user switch-off.
            else handler.postDelayed(spatialDisabledTask, 250);
        }
        @Override public void onSpatializerAvailableChanged(Spatializer source, boolean available) {
            if (source == spatializer && available) syncSpatialProfile();
        }
    };
    private int user;
    private volatile int generation;
    private volatile boolean restoring;
    private volatile boolean stopped;
    private boolean systemReceiverRegistered;
    private boolean reloadReceiverRegistered;
    private final Runnable connectTask = this::connect;
    private final Runnable spatialDisabledTask = () -> syncSpatialProfile(true);

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
                boolean firstStart = !settings.hasSavedProfile(user);
                int initialProfile = firstStart
                        && control.getNumOfProfiles() > SpatialAudioProfile.ID
                        && SpatialAudioProfile.isEnabledForActiveBluetoothDevice(this)
                        ? SpatialAudioProfile.ID : 0;
                // The OEM service only captures native defaults on first run. Replaying
                // every queried value while SWDAP is coming up can crash the vendor HAL.
                boolean capturedNativeDefaults = settings.initialize(control, user);
                if (firstStart) settings.initializeSelection(control, user, initialProfile);
                else if (!capturedNativeDefaults) settings.restore(control, user);
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
            settings.saveSelection(effect, user);
            if (SpatialAudioProfile.isSupported()) {
                spatializer = SpatialAudioProfile.getSpatializer(this);
                spatializer.addOnSpatializerStateChangedListener(handler::post, spatialListener);
                deviceAudioManager = getSystemService(AudioManager.class);
                bluetoothAudioConnected = SpatialAudioProfile.isBluetoothAudioConnected(this);
                if (deviceAudioManager != null) {
                    deviceAudioManager.registerAudioDeviceCallback(audioDeviceCallback, handler);
                }
                // Adopt an already-enabled system switch before applying the saved profile.
                if (effect.getDsOn() && SpatialAudioProfile.isEnabledForActiveBluetoothDevice(this)) syncSpatialProfile();
                else applySpatialProfile();
            }
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
                    && (!SpatialAudioProfile.isVisible(update.value)
                    || update.value >= effect.getNumOfProfiles())) return;
            if ("profile_change".equals(update.event)) {
                if (effect.getProfile() != update.value) return;
                // Profile commands already apply their spatial state at the source.
                // A delayed HAL echo is a notification, not another user command:
                // replaying it can undo a newer change made in Bluetooth Settings.
            }
            if ("ds_state_change".equals(update.event)) {
                boolean powered = effect.getDsOn();
                if (powered != (update.value > 0)) return;
                if (!powered) {
                    handler.removeCallbacks(spatialDisabledTask);
                    SpatialAudioProfile.applyDolbyPower(this, effect);
                }
            }
            settings.save(effect, user, update);
            sendUpdate(update.event, update.value);
        } catch (RuntimeException e) {
            Log.w(TAG, "Cannot process Dolby parameter update", e);
            reconnect();
        }
    }

    private void onAudioDevicesChanged() {
        if (stopped || effect == null || spatializer == null) return;
        boolean connected = SpatialAudioProfile.isBluetoothAudioConnected(this);
        if (connected == bluetoothAudioConnected) return;
        bluetoothAudioConnected = connected;
        applySpatialProfile();
    }

    private void applySpatialProfile() {
        if (stopped || effect == null || spatializer == null) return;
        try {
            if (effect.getDsOn()) SpatialAudioProfile.apply(this, effect.getProfile());
            else SpatialAudioProfile.applyDolbyPower(this, effect);
        } catch (RuntimeException e) {
            Log.w(TAG, "Cannot apply Spatial Audio connection state", e);
        }
    }

    private void syncSpatialProfile() {
        syncSpatialProfile(false);
    }

    private void syncSpatialProfile(boolean restoreOnDisable) {
        if (stopped || effect == null || spatializer == null) return;
        try {
            // Losing Bluetooth can turn the system switch off. Keep the user's profile
            // so the device callback can re-enable spatialization on reconnection.
            if (!SpatialAudioProfile.isBluetoothAudioConnected(this)) {
                if (spatializer.isEnabled()) applySpatialProfile();
                return;
            }
            boolean enabled = spatializer.isEnabled();
            // A speaker/wired route may be spatialized while an idle BT device is
            // connected. That must not select the Bluetooth-only Dolby profile.
            if (enabled && !SpatialAudioProfile.isEnabledForActiveBluetoothDevice(this)) return;
            int current = effect.getProfile();
            // Route loss (including a temporary call route) is not a user switch-off.
            if (!enabled && (!effect.getDsOn() || !restoreOnDisable || !spatializer.isAvailable()
                    || current != SpatialAudioProfile.ID)) return;
            int target = enabled ? SpatialAudioProfile.ID
                    : settings.lastNonSpatialProfile(user, effect.getNumOfProfiles());
            if (current == target && (!enabled || effect.getDsOn())) return;
            // The observer handle has low priority; take control only for this transition.
            DolbyAudioEffect control = new DolbyAudioEffect(1, 0);
            try {
                if (!control.hasControl()) {
                    Log.w(TAG, "Cannot synchronize Spatial Audio: Dolby control unavailable");
                    return;
                }
                // Capture the source profile before a system-originated transition.
                settings.saveSelection(control, user);
                if (enabled && !control.getDsOn()) control.setDsOn(true);
                if (control.getProfile() != target) {
                    control.setProfile(target);
                }
                if ((enabled && !control.getDsOn()) || control.getProfile() != target) {
                    Log.w(TAG, "Dolby did not accept the system Spatial Audio selection");
                    return;
                }
                // Do not rely on the vendor HAL echoing writes to its callback client.
                settings.saveSelection(control, user);
                sendUpdate("ds_state_change", control.getDsOn() ? 1 : 0);
                sendUpdate("profile_change", target);
            } finally {
                control.release();
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Cannot synchronize Spatial Audio profile", e);
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
        handler.removeCallbacks(spatialDisabledTask);
        if (deviceAudioManager != null) {
            deviceAudioManager.unregisterAudioDeviceCallback(audioDeviceCallback);
            deviceAudioManager = null;
        }
        if (spatializer != null) {
            try { spatializer.removeOnSpatializerStateChangedListener(spatialListener); }
            catch (RuntimeException e) { Log.d(TAG, "Spatializer listener already disconnected"); }
            spatializer = null;
        }
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
