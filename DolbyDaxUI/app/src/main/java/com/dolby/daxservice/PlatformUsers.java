package com.dolby.daxservice;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.UserHandle;
import android.util.Log;

/** Hidden platform calls isolated from the UI and public SDK build. */
final class PlatformUsers {
    private PlatformUsers() {}

    static int currentUser() {
        try {
            return (Integer) ActivityManager.class.getMethod("getCurrentUser").invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot determine active Android user", e);
        }
    }

    static void registerSystemReceiver(Context context, BroadcastReceiver receiver,
            IntentFilter filter, Handler handler) {
        try {
            Context.class.getMethod("registerReceiverAsUser", BroadcastReceiver.class,
                    UserHandle.class, IntentFilter.class, String.class, Handler.class)
                    .invoke(context, receiver, UserHandle.class.getField("ALL").get(null),
                            filter, null, handler);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot monitor Android users", e);
        }
    }

    static void sendToUser(Context context, Intent intent, int userId) {
        try {
            UserHandle user = (UserHandle) UserHandle.class.getMethod("of", int.class)
                    .invoke(null, userId);
            Context.class.getMethod("sendBroadcastAsUser", Intent.class, UserHandle.class)
                    .invoke(context, intent.setPackage(context.getPackageName()), user);
        } catch (ReflectiveOperationException e) {
            Log.e("DaxService", "Cannot deliver Dolby update to user " + userId, e);
        }
    }
}
