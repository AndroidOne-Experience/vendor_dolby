package com.dolby.dax;


import java.util.HashMap;
import java.util.Map;

public enum DsParams {
    DolbyHeadphoneVirtualizerControl(101),
    DolbyVirtualSpeakerVirtualizerControl(102),
    DolbyVolumeLevelerEnable(103),
    IntelligentEqualizerPreset(104),
    DialogEnhancementEnable(105),
    GraphicEqualizerEnable(106),
    IntelligentEqualizerAmount(107),
    DialogEnhancementAmount(108),
    DialogEnhancementDucking(109),
    GraphicEqualizerBandGains(110),
    BassEnable(111);

    private static final String[] DAP_PARAM_NAMES;
    public static final Map<Integer, Integer> kParamToLen;
    private static final DsParams[] params;
    private int id_;

    static {
        DsParams dsParams = DolbyHeadphoneVirtualizerControl;
        DsParams dsParams2 = DolbyVirtualSpeakerVirtualizerControl;
        DsParams dsParams3 = DolbyVolumeLevelerEnable;
        DsParams dsParams4 = IntelligentEqualizerPreset;
        DsParams dsParams5 = DialogEnhancementEnable;
        DsParams dsParams6 = GraphicEqualizerEnable;
        DsParams dsParams7 = IntelligentEqualizerAmount;
        DsParams dsParams8 = DialogEnhancementAmount;
        DsParams dsParams9 = DialogEnhancementDucking;
        DsParams dsParams10 = GraphicEqualizerBandGains;
        DsParams dsParams11 = BassEnable;
        DAP_PARAM_NAMES = new String[]{"null", "vdhe", "vspe", "dvle", "ieid", "deon", "geon", "iea", "dea", "ded", "gebg", "beon"};
        params = new DsParams[]{dsParams, dsParams2, dsParams3, dsParams4, dsParams5, dsParams6, dsParams7, dsParams8, dsParams9, dsParams10, dsParams11};
        kParamToLen = new HashMap<Integer, Integer>() { // from class: com.dolby.dax.DsParams.1
            {
                put(Integer.valueOf(DsParams.GraphicEqualizerBandGains.toInt()), 20);
            }
        };
    }

    DsParams(int i) {
        this.id_ = i;
    }

    public int toInt() {
        return this.id_;
    }

    @Override // java.lang.Enum
    public String toString() {
        int i = this.id_;
        return (i <= 100 || i >= 112) ? "error" : DAP_PARAM_NAMES[i - 100];
    }
}
