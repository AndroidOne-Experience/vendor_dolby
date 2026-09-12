package com.dolby.daxappui;

import android.util.Log;
import com.dolby.dax.DolbyAudioEffect;

public class DsClientSettings {
    private static final String TAG = "DsClientSettings";
    public static final DsClientSettings INSTANCE = new DsClientSettings();
    private int[] mValues = new int[1];

    DsClientSettings() {
    }

    void setDialogEnhancerOn(IDsFragObserver iDsFragObserver, boolean z) {
        if (iDsFragObserver == null) return;
        DolbyAudioEffect dolbyAudioEffect = iDsFragObserver.getDolbyAudioEffect();
        if (dolbyAudioEffect == null) {
            return;
        }
        try {
            if (dolbyAudioEffect.hasControl()) {
                dolbyAudioEffect.setDialogEnhancerEnabled(z);
            } else {
                Log.w(TAG, "dolbyAudio is out of control in setDialogEnhancerOn()");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in setDialogEnhancerOn", e);
        }
    }

    boolean getDialogEnhancerOn(IDsFragObserver iDsFragObserver) {
        if (iDsFragObserver == null) return false;
        DolbyAudioEffect dolbyAudioEffect = iDsFragObserver.getDolbyAudioEffect();
        if (dolbyAudioEffect == null) return false;
        try {
            return dolbyAudioEffect.getDialogEnhancerEnabled();
        } catch (Exception e) {
            Log.e(TAG, "Error in getDialogEnhancerOn", e);
            return false;
        }
    }

    void setDialogEnhancerAmount(IDsFragObserver iDsFragObserver, int i) {
        if (iDsFragObserver == null) return;
        DolbyAudioEffect dolbyAudioEffect = iDsFragObserver.getDolbyAudioEffect();
        if (dolbyAudioEffect == null) return;
        try {
            if (dolbyAudioEffect.getDialogEnhancerAmount() == i) {
                return;
            }
            if (dolbyAudioEffect.hasControl()) {
                dolbyAudioEffect.setDialogEnhancerAmount(i);
            } else {
                Log.w(TAG, "Dolby audio effect is out of control in setDialogEnhancerAmount");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in setDialogEnhancerAmount", e);
        }
    }

    int getDialogEnhancerAmount(IDsFragObserver iDsFragObserver) {
        if (iDsFragObserver == null) return 0;
        DolbyAudioEffect dolbyAudioEffect = iDsFragObserver.getDolbyAudioEffect();
        if (dolbyAudioEffect == null) {
            return 0;
        }
        try {
            return dolbyAudioEffect.getDialogEnhancerAmount();
        } catch (Exception e) {
            Log.e(TAG, "Error in getDialogEnhancerAmount", e);
            return 0;
        }
    }

    boolean getGraphicEqualizerOn(IDsFragObserver iDsFragObserver, int i) {
        if (iDsFragObserver == null) return false;
        DolbyAudioEffect dolbyAudioEffect = iDsFragObserver.getDolbyAudioEffect();
        if (dolbyAudioEffect == null) return false;
        try {
            return dolbyAudioEffect.isGeqEnabled(i);
        } catch (Exception e) {
            Log.e(TAG, "Error in getGraphicEqualizerOn", e);
            return false;
        }
    }

    boolean setSpeakerVirtualizerOn(IDsFragObserver context, boolean enable) {
        if (context == null) return false;
        DolbyAudioEffect dolbyAudio = context.getDolbyAudioEffect();
        if (dolbyAudio == null) {
            return false;
        }
        try {
            this.mValues[0] = !enable ? 0 : 1;
            dolbyAudio.setSpeakerVirtualizerEnabled(enable);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error in setSpeakerVirtualizerOn", e);
            return false;
        }
    }

    void setGraphicEqualizerBandGains(IDsFragObserver iDsFragObserver, int[] iArr) {
        if (iDsFragObserver == null) return;
        DolbyAudioEffect dolbyAudioEffect = iDsFragObserver.getDolbyAudioEffect();
        if (dolbyAudioEffect == null) {
            return;
        }
        try {
            if (dolbyAudioEffect.hasControl()) {
                dolbyAudioEffect.setGeqBandGains(iArr);
            } else {
                Log.w(TAG, "Dolby audio effect is out of control in setGraphicEqualizerBandGains");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in setGraphicEqualizerBandGains", e);
        }
    }

    int[] getGraphicEqualizerBandGains(IDsFragObserver iDsFragObserver) {
        if (iDsFragObserver == null) return null;
        DolbyAudioEffect dolbyAudioEffect = iDsFragObserver.getDolbyAudioEffect();
        if (dolbyAudioEffect == null) {
            return null;
        }
        try {
            return dolbyAudioEffect.getGeqBandGains();
        } catch (Exception e) {
            Log.e(TAG, "Error in getGraphicEqualizerBandGains", e);
            return null;
        }
    }

    void setBassEnhancerOn(IDsFragObserver iDsFragObserver, boolean z) {
        if (iDsFragObserver == null) return;
        DolbyAudioEffect dolbyAudioEffect = iDsFragObserver.getDolbyAudioEffect();
        if (dolbyAudioEffect == null) {
            return;
        }
        try {
            dolbyAudioEffect.setBassEnhancerEnabled(z);
        } catch (Exception e) {
            Log.e(TAG, "Error in setBassEnhancerOn", e);
        }
    }

    void setVolumeLevelerOn(IDsFragObserver iDsFragObserver, boolean z) {
        if (iDsFragObserver == null) return;
        DolbyAudioEffect dolbyAudioEffect = iDsFragObserver.getDolbyAudioEffect();
        if (dolbyAudioEffect == null) {
            return;
        }
        try {
            dolbyAudioEffect.setVolumeLevelerEnabled(z);
        } catch (Exception e) {
            Log.e(TAG, "Error in setVolumeLevelerOn", e);
        }
    }

    void setIeqPreset(IDsFragObserver iDsFragObserver, int i) {
        if (iDsFragObserver == null) return;
        DolbyAudioEffect dolbyAudioEffect = iDsFragObserver.getDolbyAudioEffect();
        if (dolbyAudioEffect == null) {
            return;
        }
        try {
            if (i == 3) {
                i = -1;
            }
            int i2 = i + 1;
            if (dolbyAudioEffect.getIeqPreset() != i2) {
                if (dolbyAudioEffect.hasControl()) {
                    dolbyAudioEffect.setIeqPreset(i2);
                } else {
                    Log.w(TAG, "Dolby audio effect is out of control in setIeqPreset");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in setIeqPreset", e);
        }
    }

    boolean getBassEnhancerOn(IDsFragObserver iDsFragObserver) {
        if (iDsFragObserver == null) return false;
        DolbyAudioEffect dolbyAudioEffect = iDsFragObserver.getDolbyAudioEffect();
        if (dolbyAudioEffect == null) {
            return false;
        }
        try {
            return dolbyAudioEffect.getBassEnhancerEnabled();
        } catch (Exception e) {
            Log.e(TAG, "Error in getBassEnhancerOn", e);
            return false;
        }
    }

    boolean getSpeakerVirtualizerOn(IDsFragObserver iDsFragObserver) {
        if (iDsFragObserver == null) return false;
        DolbyAudioEffect dolbyAudioEffect = iDsFragObserver.getDolbyAudioEffect();
        if (dolbyAudioEffect == null) {
            return false;
        }
        try {
            return dolbyAudioEffect.getSpeakerVirtualizerEnabled();
        } catch (Exception e) {
            Log.e(TAG, "Error in getSpeakerVirtualizerOn", e);
            return false;
        }
    }

    boolean getVolumeLevelerOn(IDsFragObserver iDsFragObserver) {
        if (iDsFragObserver == null) return false;
        DolbyAudioEffect dolbyAudioEffect = iDsFragObserver.getDolbyAudioEffect();
        if (dolbyAudioEffect == null) {
            return false;
        }
        try {
            return dolbyAudioEffect.getVolumeLevelerEnabled();
        } catch (Exception e) {
            Log.e(TAG, "Error in getVolumeLevelerOn", e);
            return false;
        }
    }

    int getIeqPreset(IDsFragObserver iDsFragObserver) {
        if (iDsFragObserver == null) return 3;
        DolbyAudioEffect effect = iDsFragObserver.getDolbyAudioEffect();
        if (effect == null) return 3;
        try {
            int preset = effect.getIeqPreset() - 1;
            return preset == -1 ? 3 : preset;
        } catch (Exception e) {
            Log.e(TAG, "Error in getIeqPreset", e);
            return 3;
        }
    }

    boolean isProfileSpecificSettingsModified(IDsFragObserver iDsFragObserver, int i) {
        if (iDsFragObserver == null) return false;
        DolbyAudioEffect dolbyAudioEffect = iDsFragObserver.getDolbyAudioEffect();
        if (dolbyAudioEffect == null) return false;
        try {
            return dolbyAudioEffect.isProfileSpecificSettingsModified(i);
        } catch (Exception e) {
            Log.e(TAG, "Error in isProfileSpecificSettingsModified", e);
            return false;
        }
    }

    void resetProfileSpecificSettings(IDsFragObserver iDsFragObserver, int i) {
        if (iDsFragObserver == null) return;
        DolbyAudioEffect dolbyAudioEffect = iDsFragObserver.getDolbyAudioEffect();
        if (dolbyAudioEffect == null) {
            return;
        }
        try {
            if (dolbyAudioEffect.hasControl()) {
                dolbyAudioEffect.resetProfileSpecificSettings(i);
            } else {
                Log.w(TAG, "Dolby audio effect is out of control in resetProfileSpecificSettings");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in resetProfileSpecificSettings", e);
        }
    }

    public void resetAllProfileSpecificSettings(IDsFragObserver iDsFragObserver) {
        if (iDsFragObserver == null) return;
        DolbyAudioEffect dolbyAudioEffect = iDsFragObserver.getDolbyAudioEffect();
        if (dolbyAudioEffect == null) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            try {
                if (dolbyAudioEffect.hasControl()) {
                    dolbyAudioEffect.resetProfileSpecificSettings(i);
                } else {
                    Log.w(TAG, "Dolby audio effect is out of control in resetAllProfileSpecificSettings");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in resetAllProfileSpecificSettings for profile " + i, e);
            }
        }
    }
}
