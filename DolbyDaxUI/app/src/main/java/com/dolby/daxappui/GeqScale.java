package com.dolby.daxappui;

final class GeqScale {
    private GeqScale() {}

    static float clamp(float gain, float min, float max) {
        return Math.max(min, Math.min(max, gain));
    }

    static int toUnits(float gain) {
        return Math.round(gain * 16.0f);
    }

    static float fromUnits(int units) {
        return units / 16.0f;
    }

    static float gainToY(float gain, float top, float height, float min, float max) {
        return top + (max - clamp(gain, min, max)) * height / (max - min);
    }

    static float dragGain(float gain, float deltaY, float height, float min, float max) {
        if (height <= 0) return gain;
        return clamp(gain + deltaY * (max - min) / height, min, max);
    }
}
