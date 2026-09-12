package com.dolby.daxappui;

import android.content.Context;
import android.util.Log;

class DAXConfiguration {
    private static DAXConfiguration dynamicInstance;
    private float maxEditGain;
    private float minEditGain;

    private DAXConfiguration(Context context) {
        this.minEditGain = -12.0f;
        this.maxEditGain = 12.0f;
        try {
            this.minEditGain = Float.parseFloat(context.getResources().getString(R.string.min_edit_gain));
            this.maxEditGain = Float.parseFloat(context.getResources().getString(R.string.max_edit_gain));
        } catch (NullPointerException unused) {
            this.minEditGain = Float.NaN;
            this.maxEditGain = Float.NaN;
            Log.e("DAXConfiguration", "Some of values from configuration.xml were not loaded!");
        } catch (NumberFormatException unused2) {
            this.minEditGain = Float.NaN;
            this.maxEditGain = Float.NaN;
            Log.e("DAXConfiguration", "Some of values from configuration.xml were not float type!");
        }
    }

    static DAXConfiguration getInstance(Context context) {
        if (dynamicInstance == null) {
            dynamicInstance = new DAXConfiguration(context);
        }
        return dynamicInstance;
    }

    float getMaxEditGain() {
        if (Float.isNaN(this.maxEditGain)) {
            return 12.0f;
        }
        return this.maxEditGain;
    }

    float getMinEditGain() {
        if (Float.isNaN(this.minEditGain)) {
            return -12.0f;
        }
        return this.minEditGain;
    }
}
