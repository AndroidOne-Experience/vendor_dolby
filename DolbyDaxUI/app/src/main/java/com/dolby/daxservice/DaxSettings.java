package com.dolby.daxservice;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;
import com.dolby.dax.DolbyAudioEffect;
import com.dolby.dax.SpatialAudioProfile;
import org.json.JSONArray;
import org.json.JSONException;

/** Device-protected, per-user copies of the settings exposed by the DAX UI. */
final class DaxSettings {
    private static final int SCHEMA_VERSION = 2;
    private static final int IEQ_PRESET = 104;
    private static final int DIALOG_ENHANCEMENT_AMOUNT = 108;
    private static final int GEQ_BAND_GAINS = 110;
    private static final int[] PARAMETERS = {
            IEQ_PRESET, DIALOG_ENHANCEMENT_AMOUNT, GEQ_BAND_GAINS
    };
    private final Context storage;

    DaxSettings(Context context) { storage = context.createDeviceProtectedStorageContext(); }

    private SharedPreferences prefs(int user) {
        return storage.getSharedPreferences("dax_user_" + user, Context.MODE_PRIVATE);
    }

    void remove(int user) { storage.deleteSharedPreferences("dax_user_" + user); }

    boolean hasSavedProfile(int user) { return prefs(user).contains("profile"); }

    void initializeSelection(DolbyAudioEffect effect, int user, int profile) {
        boolean power = profile == SpatialAudioProfile.ID || effect.getDsOn();
        // Save the intended selection before applying it so a backend retry restores
        // the same first-run choice rather than the native engine's initial profile.
        prefs(user).edit().putInt("profile", profile).putBoolean("power", power)
                .putInt("last_non_spatial_profile", 0).apply();
        if (power != effect.getDsOn()) effect.setDsOn(power);
        if (effect.getProfile() != profile) effect.setProfile(profile);
    }

    /** Returns true when native state was captured and must not be replayed. */
    boolean initialize(DolbyAudioEffect effect, int user) {
        SharedPreferences defaults = prefs(-1);
        if (defaults.getInt("schema_version", 0) == SCHEMA_VERSION) {
            // Extend existing defaults without discarding the user's four Dolby profiles.
            if (SpatialAudioProfile.isSupported() && effect.getNumOfProfiles() > SpatialAudioProfile.ID
                    && !defaults.contains(key(SpatialAudioProfile.ID, GEQ_BAND_GAINS))) {
                SharedPreferences.Editor editor = defaults.edit();
                snapshotProfile(effect, editor, SpatialAudioProfile.ID);
                editor.apply();
            }
            return false;
        }

        // Version 1 persisted parameters the OEM service never restored. Discard that
        // state and establish a clean baseline without writing anything back to SWDAP.
        SharedPreferences.Editor defaultEditor = defaults.edit().clear();
        snapshot(effect, defaultEditor);
        defaultEditor.putInt("schema_version", SCHEMA_VERSION).apply();

        SharedPreferences.Editor userEditor = prefs(user).edit().clear();
        snapshot(effect, userEditor);
        userEditor.apply();
        return true;
    }

    private void snapshot(DolbyAudioEffect effect, SharedPreferences.Editor editor) {
        editor.putBoolean("power", effect.getDsOn());
        putProfile(editor, effect.getProfile());
        for (int profile = 0; profile < Math.min(SpatialAudioProfile.count(), effect.getNumOfProfiles()); profile++) {
            snapshotProfile(effect, editor, profile);
        }
    }

    private void snapshotProfile(DolbyAudioEffect effect, SharedPreferences.Editor editor, int profile) {
        for (int param : PARAMETERS) {
            if (!isPersistedForProfile(profile, param)) continue;
            try {
                int[] values = effect.getDapParameter(profile, param);
                JSONArray array = new JSONArray();
                for (int value : values) array.put(value);
                editor.putString(key(profile, param), array.toString());
            } catch (RuntimeException e) {
                if (param == GEQ_BAND_GAINS) throw e;
                Log.d("DaxService", "Cannot read profile " + profile + " parameter " + param);
            }
        }
    }

    void save(DolbyAudioEffect effect, int user, DapUpdate update) {
        SharedPreferences.Editor editor = prefs(user).edit();
        if ("ds_state_change".equals(update.event)) {
            editor.putBoolean("power", update.value > 0);
        } else if ("profile_change".equals(update.event)) {
            if (update.value < 0 || update.value >= Math.min(SpatialAudioProfile.count(), effect.getNumOfProfiles())) return;
            putProfile(editor, update.value);
        } else {
            if (update.value < 0 || update.value >= Math.min(SpatialAudioProfile.count(), effect.getNumOfProfiles())) return;
            // Read the actual post-reset values, not guessed defaults.
            snapshotProfile(effect, editor, update.value);
        }
        editor.apply();
    }

    void saveSelection(DolbyAudioEffect effect, int user) {
        SharedPreferences.Editor editor = prefs(user).edit();
        editor.putBoolean("power", effect.getDsOn());
        putProfile(editor, effect.getProfile());
        editor.apply();
    }

    private static void putProfile(SharedPreferences.Editor editor, int profile) {
        editor.putInt("profile", profile);
        if (profile >= 0 && profile < SpatialAudioProfile.ID) {
            editor.putInt("last_non_spatial_profile", profile);
        }
    }

    int lastNonSpatialProfile(int user, int profileCount) {
        SharedPreferences saved = prefs(user);
        int profile = saved.getInt("last_non_spatial_profile", saved.getInt("profile", 0));
        // Older installations may have no non-spatial history yet.
        return profile >= 0 && profile < Math.min(SpatialAudioProfile.ID, profileCount)
                ? profile : 0;
    }

    void saveAll(DolbyAudioEffect effect, int user) {
        SharedPreferences.Editor editor = prefs(user).edit();
        snapshot(effect, editor);
        editor.apply();
    }

    void restore(DolbyAudioEffect effect, int user) {
        SharedPreferences saved = prefs(user);
        SharedPreferences defaults = prefs(-1);
        if (defaults.getInt("schema_version", 0) != SCHEMA_VERSION) return;
        // Match the OEM service's proven ordering and parameter subset. In particular,
        // do not replay virtualizer/leveler/enable parameters discovered by readback.
        effect.setDsOn(saved.getBoolean("power", defaults.getBoolean("power", true)));
        int count = Math.min(SpatialAudioProfile.count(), effect.getNumOfProfiles());
        for (int profile = 0; profile < count; profile++) {
            for (int param : PARAMETERS) {
                if (!isPersistedForProfile(profile, param)) continue;
                String key = key(profile, param);
                String encoded = saved.getString(key, defaults.getString(key, null));
                if (encoded == null) continue;
                try {
                    JSONArray array = new JSONArray(encoded);
                    int length = param == GEQ_BAND_GAINS ? 20 : 1;
                    if (array.length() != length) continue;
                    int[] values = new int[length];
                    for (int i = 0; i < length; i++) values[i] = array.getInt(i);
                    effect.setDapParameter(profile, param, values);
                } catch (JSONException | RuntimeException e) {
                    Log.w("DaxService", "Cannot restore " + key, e);
                }
            }
        }
        // The decompiled OEM service lets SWDAP finish applying profile parameters
        // before selecting the active profile.
        SystemClock.sleep(300);
        int profile = saved.getInt("profile", defaults.getInt("profile", 0));
        if (profile >= 0 && profile < count) effect.setProfile(profile);
    }

    private static boolean isPersistedForProfile(int profile, int param) {
        if (param == GEQ_BAND_GAINS) return true;
        if (param == DIALOG_ENHANCEMENT_AMOUNT) return profile == 1 || profile == 3;
        return param == IEQ_PRESET && (profile == 2 || profile == 3 || profile == SpatialAudioProfile.ID);
    }

    private static String key(int profile, int param) { return "profile_" + profile + "_" + param; }
}
