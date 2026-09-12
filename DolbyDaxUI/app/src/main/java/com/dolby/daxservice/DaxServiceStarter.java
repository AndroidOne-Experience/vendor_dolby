package com.dolby.daxservice;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.util.Log;

/** The persistent owner-user application starts the single system-wide backend. */
public final class DaxServiceStarter extends BroadcastReceiver {
    public static void start(Context context) {
        if (Process.myUid() / 100000 != 0
                || Application.getProcessName().endsWith(":daxservice")) return;
        try {
            context.startService(new Intent(context, DaxService.class));
        } catch (RuntimeException e) {
            Log.e("DaxService", "Unable to start system Dolby backend", e);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            Log.i("DaxService", "Received action " + action + ", starting DaxService");
            start(context);
        }
    }
}
