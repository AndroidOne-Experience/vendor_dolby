package com.dolby.dax;

public enum DsTuning {
    internal_speaker(0),
    hdmi(1),
    miracast(2),
    headphone(3),
    bluetooth(4),
    usb(5);

    public static final int[] PORT_IDS;
    public static final String[] PORT_NAMES;
    private int id_;

    static {
        DsTuning dsTuning = internal_speaker;
        PORT_NAMES = new String[]{"internal_speaker", "hdmi", "miracast", "headphone", "bluetooth", "usb", "other"};
        PORT_IDS = new int[]{dsTuning.toInt(), hdmi.toInt(), miracast.toInt(), headphone.toInt(), bluetooth.toInt(), usb.toInt()};
    }

    DsTuning(int i) {
        this.id_ = i;
    }

    public int toInt() {
        return this.id_;
    }

    @Override // java.lang.Enum
    public String toString() {
        return PORT_NAMES[this.id_];
    }
}
