package com.dolby.daxappui.soundsettings;

import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import com.dolby.dax.DolbyAudioEffect;
import com.dolby.daxappui.R;
import com.dolby.daxservice.DaxService;

/** Implements SettingsLib's dynamic summary and switch ContentProvider protocol. */
public final class DaxSettingsProvider extends ContentProvider {
    private static final String TAG = "DaxSettingsProvider";
    private static final String BASE_URI = "content://com.dolby.daxappui.settings/";
    private static final String KEY = "dolby_atmos";
    private static final String SUMMARY = "com.android.settings.summary";
    private static final int[] PROFILE_NAMES = {
            R.string.dynamic, R.string.movie, R.string.music, R.string.custom
    };

    private final BroadcastReceiver updates = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            notifySettings();
        }
    };

    @Override public boolean onCreate() {
        // The backend sends these to the active user, including changes from the app,
        // Quick Settings and reconnection after an audio-server restart.
        IntentFilter filter = new IntentFilter("com.dolby.intent.action.DAP_PARAMS_UPDATE");
        if (Build.VERSION.SDK_INT >= 33) {
            getContext().registerReceiver(updates, filter, DaxService.PERMISSION, null,
                    Context.RECEIVER_EXPORTED);
        } else {
            getContext().registerReceiver(updates, filter, DaxService.PERMISSION, null);
        }
        return true;
    }

    private void notifySettings() {
        getContext().getContentResolver().notifyChange(
                Uri.parse(BASE_URI + "getDynamicSummary/" + KEY), null);
        getContext().getContentResolver().notifyChange(
                Uri.parse(BASE_URI + "isChecked/" + KEY), null);
    }

    @Override public synchronized Bundle call(String method, String arg, Bundle extras) {
        if (!"getDynamicSummary".equals(method) && !"isChecked".equals(method)
                && !"onCheckedChanged".equals(method)) return null;
        if (extras != null && extras.containsKey("com.android.settings.keyhint")
                && !KEY.equals(extras.getString("com.android.settings.keyhint"))) return null;
        boolean changing = "onCheckedChanged".equals(method);
        Bundle result = new Bundle();
        DolbyAudioEffect effect = null;
        try {
            // Queries never take control away from the UI or change the active profile.
            effect = new DolbyAudioEffect(changing ? 1 : -1, 0);
            if (changing) {
                if (extras == null || !extras.containsKey("checked_state")) {
                    throw new IllegalArgumentException("Missing checked_state");
                }
                if (!effect.hasControl()) throw new IllegalStateException("Dolby is busy");
                boolean enabled = extras.getBoolean("checked_state");
                effect.setDsOn(enabled);
                result.putBoolean("set_checked_error", effect.getDsOn() != enabled);
                notifySettings();
            } else if ("isChecked".equals(method)) {
                result.putBoolean("checked_state", effect.getDsOn());
            } else {
                int profile = effect.getDsOn() ? effect.getProfile() : -1;
                result.putString(SUMMARY, getContext().getString(
                        profile >= 0 && profile < PROFILE_NAMES.length
                                ? PROFILE_NAMES[profile] : R.string.dolby_settings_summary));
            }
        } catch (RuntimeException | LinkageError e) {
            Log.w(TAG, "Cannot access Dolby state", e);
            if (changing) {
                result.putBoolean("set_checked_error", true);
                result.putString("set_checked_error_message",
                        getContext().getString(R.string.dolby_settings_unavailable));
            } else if ("isChecked".equals(method)) {
                result.putBoolean("checked_state", false);
            } else {
                result.putString(SUMMARY, getContext().getString(R.string.dolby_settings_summary));
            }
        } finally {
            if (effect != null) {
                try { effect.release(); }
                catch (RuntimeException e) { Log.w(TAG, "Cannot release Dolby effect", e); }
            }
        }
        return result;
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) { throw new UnsupportedOperationException(); }
    @Override public String getType(Uri uri) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] args) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String selection,
            String[] args) { throw new UnsupportedOperationException(); }
}
