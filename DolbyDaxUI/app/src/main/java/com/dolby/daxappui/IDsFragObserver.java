package com.dolby.daxappui;

import com.dolby.dax.DolbyAudioEffect;

public interface IDsFragObserver {
    void chooseProfile(int i);

    int getActivePort();

    DolbyAudioEffect getDolbyAudioEffect();

    void resetProfile(int i);

    /** Immediately mirrors a successful Dolby power mutation into the visible UI. */
    void dsPowerChanged(boolean enabled);

    /** Recomputes lightweight UI derived from whether a profile has unsaved/default changes. */
    void profileModificationChanged(int profile);

    /** Re-read the active profile from the engine and repaint its complete panel. */
    void profileSettingsChanged(int profile);
}
