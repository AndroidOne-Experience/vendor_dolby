package com.dolby.dax;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioDeviceInfo;
import android.media.Spatializer;
import android.os.Build;
import android.os.SystemProperties;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;

/** Native profile IDs remain independent of the order of the visible tabs. */
public final class SpatialAudioProfile {
    public static final int ID = 4;
    private SpatialAudioProfile() {}

    public static boolean isSupported() {
        return Build.VERSION.SDK_INT >= 32
                && "true".equals(SystemProperties.get("ro.audio.stereo_spatialization_enabled", "false"))
                && "true".equals(SystemProperties.get("ro.audio.spatializer_enabled", "false"));
    }

    public static int count() { return isSupported() ? 5 : 4; }
    public static boolean isVisible(int profile) { return profile >= 0 && profile < count(); }
    public static int fromPosition(int position) {
        return isSupported() ? (position == 0 ? ID : position - 1) : position;
    }
    public static int toPosition(int profile) {
        return isSupported() ? (profile == ID ? 0 : profile + 1) : profile;
    }

    public static Spatializer getSpatializer(Context context) {
        return context.getSystemService(AudioManager.class).getSpatializer();
    }

    /** Checks connected audio outputs, rather than merely paired Bluetooth devices. */
    public static boolean isBluetoothAudioConnected(Context context) {
        if (context == null) return false;
        try {
            AudioManager manager = context.getSystemService(AudioManager.class);
            if (manager == null) return false;
            for (AudioDeviceInfo device : manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
                if (isBluetoothDeviceType(device.getType())) return true;
            }
        } catch (RuntimeException e) {
            Log.w("SpatialAudioProfile", "Cannot check Bluetooth audio connection", e);
        }
        return false;
    }

    public static void apply(Context context, int profile) {
        if (!isSupported() || !isVisible(profile)) return;
        try {
            Spatializer spatializer = getSpatializer(context);
            if (spatializer == null) return;
            boolean connected = isBluetoothAudioConnected(context);
            AudioManager manager = context.getSystemService(AudioManager.class);
            if (manager != null) {
                for (AudioDeviceInfo device : manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
                    if (!isBluetoothDeviceType(device.getType())) continue;
                    if (profile == ID) addCompatibleAudioDevice(spatializer, device);
                    else removeCompatibleAudioDevice(spatializer, device);
                }
            }
            // Bluetooth Settings toggles device compatibility, not the global feature.
            // Keep the feature available while connected so Settings can enable a device
            // even when Dolby currently uses a non-spatial profile.
            setSpatializerEnabled(spatializer, connected);
        } catch (Exception e) {
            Log.w("SpatialAudioProfile", "Cannot apply spatial state for profile " + profile, unwrap(e));
        }
    }

    private static boolean isBluetoothDeviceType(int type) {
        switch (type) {
            case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP:
            case AudioDeviceInfo.TYPE_BLUETOOTH_SCO:
            case AudioDeviceInfo.TYPE_HEARING_AID:
            case AudioDeviceInfo.TYPE_BLE_HEADSET:
            case AudioDeviceInfo.TYPE_BLE_SPEAKER:
            case AudioDeviceInfo.TYPE_BLE_BROADCAST:
                return true;
            default:
                return false;
        }
    }

    private static Object createAudioDeviceAttributes(AudioDeviceInfo info) {
        if (info == null) return null;
        try {
            Class<?> adaClass = Class.forName("android.media.AudioDeviceAttributes");
            // Attempt 1: AudioDeviceAttributes(AudioDeviceInfo)
            try {
                return adaClass.getConstructor(AudioDeviceInfo.class).newInstance(info);
            } catch (Exception ignored) {}

            String address = info.getAddress() != null ? info.getAddress() : "";

            // Attempt 2: AudioDeviceAttributes(int role, int type, String address) - role=2 (ROLE_OUTPUT)
            try {
                return adaClass.getConstructor(int.class, int.class, String.class)
                        .newInstance(2, info.getType(), address);
            } catch (Exception ignored) {}

            // Attempt 3: AudioDeviceAttributes(int nativeType, String address)
            try {
                return adaClass.getConstructor(int.class, String.class)
                        .newInstance(info.getType(), address);
            } catch (Exception ignored) {}

            // Attempt 4: AudioDeviceAttributes(int role, int type, String address, String name)
            try {
                String name = info.getProductName() != null ? info.getProductName().toString() : "";
                return adaClass.getConstructor(int.class, int.class, String.class, String.class)
                        .newInstance(2, info.getType(), address, name);
            } catch (Exception ignored) {}

        } catch (Exception e) {
            Log.w("SpatialAudioProfile", "Cannot create AudioDeviceAttributes for " + info, unwrap(e));
        }
        return null;
    }

    private static void addCompatibleAudioDevice(Spatializer spatializer, Object device) {
        if (spatializer == null || device == null) return;
        try {
            Class<?> deviceClass = Class.forName("android.media.AudioDeviceAttributes");
            Object ada = device;
            if (!deviceClass.isInstance(ada)) {
                if (ada instanceof AudioDeviceInfo) {
                    ada = createAudioDeviceAttributes((AudioDeviceInfo) ada);
                }
            }
            if (ada == null || !deviceClass.isInstance(ada)) {
                Log.w("SpatialAudioProfile", "Object is not an instance of AudioDeviceAttributes: " + device);
                return;
            }
            try {
                Spatializer.class.getMethod("addCompatibleAudioDevice", deviceClass)
                        .invoke(spatializer, ada);
            } catch (NoSuchMethodException e) {
                try {
                    Spatializer.class.getMethod("addAudioDevice", deviceClass)
                            .invoke(spatializer, ada);
                } catch (NoSuchMethodException ignored) {}
            }
        } catch (Exception e) {
            Log.w("SpatialAudioProfile", "Cannot add compatible audio device: " + device, unwrap(e));
        }
    }

    private static void removeCompatibleAudioDevice(Spatializer spatializer, Object device) {
        if (spatializer == null || device == null) return;
        try {
            Class<?> deviceClass = Class.forName("android.media.AudioDeviceAttributes");
            Object ada = device;
            if (!deviceClass.isInstance(ada)) {
                if (ada instanceof AudioDeviceInfo) {
                    ada = createAudioDeviceAttributes((AudioDeviceInfo) ada);
                }
            }
            if (ada == null || !deviceClass.isInstance(ada)) {
                return;
            }
            try {
                Spatializer.class.getMethod("removeCompatibleAudioDevice", deviceClass)
                        .invoke(spatializer, ada);
            } catch (NoSuchMethodException e) {
                try {
                    Spatializer.class.getMethod("removeAudioDevice", deviceClass)
                            .invoke(spatializer, ada);
                } catch (NoSuchMethodException ignored) {}
            }
        } catch (Exception e) {
            Log.w("SpatialAudioProfile", "Cannot remove compatible audio device: " + device, unwrap(e));
        }
    }

    private static void setSpatializerEnabled(Spatializer spatializer, boolean enabled) throws Exception {
        try {
            Spatializer.class.getMethod("setEnabled", boolean.class)
                    .invoke(spatializer, enabled);
        } catch (NoSuchMethodException e) {
            try {
                Spatializer.class.getMethod("setIsEnabled", boolean.class)
                        .invoke(spatializer, enabled);
            } catch (NoSuchMethodException e2) {
                Spatializer.class.getMethod("setSpatializerEnabled", boolean.class)
                        .invoke(spatializer, enabled);
            }
        }
    }

    private static Throwable unwrap(Throwable t) {
        if (t instanceof InvocationTargetException && t.getCause() != null) {
            return t.getCause();
        }
        return t;
    }
}
