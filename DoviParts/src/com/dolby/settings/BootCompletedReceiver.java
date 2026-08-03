/*
 * Copyright (C) 2023 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dolby.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.util.Log;
import android.view.Display;
import android.view.Display.HdrCapabilities;

import java.util.Arrays;

public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = "DoviParts";
    private static final boolean DEBUG = true;

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }

        if (DEBUG) {
            Log.d(TAG, "Received boot completed intent");
        }

        final DisplayManager displayManager = context.getSystemService(DisplayManager.class);
        if (displayManager == null) {
            Log.e(TAG, "DisplayManager is null");
            return;
        }

        final Display display = displayManager.getDisplay(Display.DEFAULT_DISPLAY);
        if (display == null) {
            Log.e(TAG, "Default display is null");
            return;
        }

        final int[] supportedHdrTypes =
                display.getHdrCapabilities().getSupportedHdrTypes();

        boolean hasDolbyVision = false;
        for (int type : supportedHdrTypes) {
            if (type == HdrCapabilities.HDR_TYPE_DOLBY_VISION) {
                hasDolbyVision = true;
                break;
            }
        }

        if (hasDolbyVision) {
            if (DEBUG) {
                Log.d(TAG, "Dolby Vision already present, skipping override");
            }
            return;
        }

        int[] overrideHdrTypes =
                Arrays.copyOf(supportedHdrTypes, supportedHdrTypes.length + 1);
        overrideHdrTypes[supportedHdrTypes.length] =
                HdrCapabilities.HDR_TYPE_DOLBY_VISION;

        // Override HDR types to enable Dolby Vision
        displayManager.overrideHdrTypes(Display.DEFAULT_DISPLAY, overrideHdrTypes);

        if (DEBUG) {
            Log.d(TAG, "HDR types overridden: " + Arrays.toString(overrideHdrTypes));
        }
    }
}
