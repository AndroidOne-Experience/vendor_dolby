package com.dolby.daxservice;

import android.os.Handler;
import android.os.IBinder;
import android.os.ServiceManager;
import android.util.Log;

/** Watches AudioFlinger using one retained death recipient and a cancellable retry. */
final class AudioServerWatchDog {
    private final Handler handler;
    private final Runnable restarted;
    private IBinder audioFlinger;
    private boolean stopped;
    private boolean connectedOnce;
    private final Runnable connect = this::connect;
    private final IBinder.DeathRecipient deathRecipient = () -> handlerPostDeath();

    AudioServerWatchDog(Handler handler, Runnable restarted) {
        this.handler = handler;
        this.restarted = restarted;
    }

    void start() { connect(); }

    private void handlerPostDeath() {
        handler.post(() -> {
            if (stopped) return;
            audioFlinger = null;
            handler.removeCallbacks(connect);
            handler.postDelayed(connect, 1000);
        });
    }

    private void connect() {
        if (stopped) return;
        try {
            IBinder binder = ServiceManager.checkService("media.audio_flinger");
            if (binder != null) {
                binder.linkToDeath(deathRecipient, 0);
                audioFlinger = binder;
                if (connectedOnce) restarted.run();
                connectedOnce = true;
                return;
            }
        } catch (Exception e) {
            Log.w("DaxService", "AudioFlinger unavailable; retrying", e);
        }
        handler.postDelayed(connect, 1000);
    }

    void stop() {
        stopped = true;
        handler.removeCallbacks(connect);
        if (audioFlinger != null) {
            try { audioFlinger.unlinkToDeath(deathRecipient, 0); }
            catch (RuntimeException e) { Log.d("DaxService", "AudioFlinger already disconnected"); }
            audioFlinger = null;
        }
    }
}
