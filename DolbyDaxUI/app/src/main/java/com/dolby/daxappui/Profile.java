package com.dolby.daxappui;

class Profile {
    private int mIconNormal;
    private int mIconSelected;

    Profile(int i, int i2) {
        this.mIconSelected = i;
        this.mIconNormal = i2;
    }

    int getIcon(boolean z) {
        return z ? this.mIconSelected : this.mIconNormal;
    }
}
