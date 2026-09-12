package com.dolby.daxappui;

import android.app.Application;
import android.app.UiModeManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.support.v7.app.AppCompatDelegate;

public class DAXApplication extends Application {
    public static final String APPEARANCE_PREFERENCES = "appearance_preferences";
    public static final String APPEARANCE_MODE = "appearance_mode";
    public static final int APPEARANCE_DARK = 0;
    public static final int APPEARANCE_LIGHT = 1;
    public static final int APPEARANCE_SYSTEM = 2;
    static DAXApplication mDAXApplication;

    public DAXApplication() {
        mDAXApplication = this;
    }

    public static DAXApplication getInstance() {
        return mDAXApplication;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        com.dolby.daxservice.DaxServiceStarter.start(this);
        if (Application.getProcessName().endsWith(":daxservice")) return;
        android.os.UserManager users = getSystemService(android.os.UserManager.class);
        if (users != null && !users.isUserUnlocked()) return;
        applyAppearanceMode(this, getSharedPreferences(APPEARANCE_PREFERENCES, MODE_PRIVATE)
                .getInt(APPEARANCE_MODE, APPEARANCE_DARK));
    }

    public static void applyAppearanceMode(Context context, int mode) {
        if (mode == APPEARANCE_LIGHT) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (mode == APPEARANCE_SYSTEM) {
            AppCompatDelegate.setDefaultNightMode(systemUsesDarkTheme(context)
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
    }

    public static boolean systemUsesDarkTheme(Context context) {
        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
        return uiModeManager != null && uiModeManager.getNightMode() == UiModeManager.MODE_NIGHT_YES;
    }

    public String getProductVersion() {
        try {
            return getPackageManager().getPackageInfo("com.dolby.daxappui", 16384).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String[] getProfileNames() {
        return new String[]{getString(R.string.dynamic), getString(R.string.movie), getString(R.string.music), getString(R.string.custom)};
    }

    public static float getIeqViewWidth(Context context) {
        Resources resources = context.getResources();
        int windowWidth = Math.round(resources.getConfiguration().screenWidthDp
                * resources.getDisplayMetrics().density);
        int spacing = resources.getDimensionPixelOffset(R.dimen.ieq_margin_start)
                + resources.getDimensionPixelOffset(R.dimen.ieq_margin_end)
                + resources.getDimensionPixelOffset(R.dimen.ieq_horizontal_spacing_normal) * 2
                + resources.getDimensionPixelOffset(R.dimen.ieq_horizontal_spacing_off) * 3;
        return Math.max(0, windowWidth - spacing) / 4.0f;
    }
}
