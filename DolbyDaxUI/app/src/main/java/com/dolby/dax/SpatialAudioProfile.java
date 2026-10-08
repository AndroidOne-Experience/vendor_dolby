package com.dolby.dax;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.Spatializer;
import android.os.Build;
import android.os.SystemProperties;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

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

    /** Startup must inspect the media route, not merely a connected/idle Bluetooth device. */
    public static boolean isEnabledForActiveBluetoothDevice(Context context) {
        if (context == null || !isSupported()) return false;
        try {
            AudioManager manager = context.getSystemService(AudioManager.class);
            if (manager == null) return false;
            Spatializer spatializer = manager.getSpatializer();
            if (spatializer == null || !spatializer.isEnabled()) return false;
            AudioAttributes media = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA).build();
            List<?> routes;
            try {
                routes = (List<?>) AudioManager.class.getMethod("getAudioDevicesForAttributes",
                        AudioAttributes.class).invoke(manager, media);
            } catch (NoSuchMethodException e) {
                routes = (List<?>) AudioManager.class.getMethod("getDevicesForAttributes",
                        AudioAttributes.class).invoke(manager, media);
            }
            List<?> enabledDevices = (List<?>) Spatializer.class
                    .getMethod("getCompatibleAudioDevices").invoke(spatializer);
            if (routes == null || enabledDevices == null) return false;
            for (Object route : routes) {
                int type = deviceType(route);
                if (!isBluetoothDeviceType(type)) continue;
                String address = deviceAddress(route);
                for (Object enabled : enabledDevices) {
                    if (type == deviceType(enabled) && address.equals(deviceAddress(enabled))) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.w("SpatialAudioProfile", "Cannot query active Bluetooth spatial audio state", unwrap(e));
        }
        return false;
    }

    private static int deviceType(Object device) throws ReflectiveOperationException {
        if (device instanceof AudioDeviceInfo) return ((AudioDeviceInfo) device).getType();
        return (Integer) device.getClass().getMethod("getType").invoke(device);
    }

    private static String deviceAddress(Object device) throws ReflectiveOperationException {
        String address = device instanceof AudioDeviceInfo ? ((AudioDeviceInfo) device).getAddress()
                : (String) device.getClass().getMethod("getAddress").invoke(device);
        return address == null ? "" : address;
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
        apply(context, profile, true);
    }

    /** A Dolby power command affects spatialization only for the Bluetooth spatial profile. */
    public static void applyDolbyPower(Context context, DolbyAudioEffect effect) {
        if (context == null || effect == null || !isSupported()) return;
        try {
            if (effect.getProfile() == ID && isBluetoothAudioConnected(context)) {
                apply(context, ID, effect.getDsOn());
            }
        } catch (RuntimeException e) {
            Log.w("SpatialAudioProfile", "Cannot apply Dolby power to Bluetooth spatial audio", e);
        }
    }

    private static void apply(Context context, int profile, boolean dolbyEnabled) {
        if (!isSupported() || !isVisible(profile)) return;
        try {
            Spatializer spatializer = getSpatializer(context);
            if (spatializer == null) return;
            boolean connected = isBluetoothAudioConnected(context);
            AudioManager manager = context.getSystemService(AudioManager.class);
            if (manager != null) {
                for (AudioDeviceInfo device : manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
                    if (!isBluetoothDeviceType(device.getType())) continue;
                    if (profile == ID && dolbyEnabled) addCompatibleAudioDevice(spatializer, device);
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
