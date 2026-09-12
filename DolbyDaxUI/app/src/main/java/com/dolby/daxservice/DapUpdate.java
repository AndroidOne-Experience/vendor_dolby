package com.dolby.daxservice;

/** Decodes the little-endian DMS callback protocol used by the original service. */
final class DapUpdate {
    static final String ACTION = "com.dolby.intent.action.DAP_PARAMS_UPDATE";
    static final String EVENT = "event name";
    static final String VALUE = "Integer Value";
    final String event;
    final int value;

    private DapUpdate(String event, int value) {
        this.event = event;
        this.value = value;
    }

    static DapUpdate decode(byte[] data) {
        if (data == null || data.length < 8) return null;
        switch (readInt(data, 0)) {
            case 0: return new DapUpdate("ds_state_change", readInt(data, 4));
            case 0x01000000:
            case 0x02000000:
                return data.length < 12 ? null
                        : new DapUpdate("profile_setting_change", readInt(data, 8));
            case 0x0a000000: return new DapUpdate("profile_change", readInt(data, 4));
            case 0x0c000000: return new DapUpdate("reset_profile_setting", readInt(data, 4));
            default: return null;
        }
    }

    private static int readInt(byte[] data, int offset) {
        return (data[offset] & 255) | ((data[offset + 1] & 255) << 8)
                | ((data[offset + 2] & 255) << 16) | ((data[offset + 3] & 255) << 24);
    }
}
