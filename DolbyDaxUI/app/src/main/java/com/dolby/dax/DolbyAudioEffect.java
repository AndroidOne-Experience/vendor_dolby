package com.dolby.dax;

import android.media.audiofx.AudioEffect;
import android.os.SystemProperties;
import android.util.Log;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

public class DolbyAudioEffect extends AudioEffect {
    public static final UUID EFFECT_TYPE_DOLBY_AUDIO_PROCESSING;
    private static int sNumOfProfiles;
    private BaseParameterListener mBaseParamListener;
    private OnParameterChangeListener mParamListener;
    private final Object mParamListenerLock;
    private boolean mProfileSupported;
    private int mSessionId;

    // Original Smali has no constructor or methods; never instantiated.
    private abstract class BaseParameterListener implements AudioEffect.OnParameterChangeListener {
    }

    public interface OnParameterChangeListener {
    }

    static {
        EFFECT_TYPE_DOLBY_AUDIO_PROCESSING = UUID.fromString(!SystemProperties.get("ro.dolby.mod_uuid", "false").equals("true") ? "9d4921da-8225-4f29-aefa-39537a04bcaa" : "9d4921da-8225-4f29-aefa-5f7279756b69");
        sNumOfProfiles = 0;
    }

    public DolbyAudioEffect(int i, int i2) throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException, RuntimeException {
        super(AudioEffect.EFFECT_TYPE_NULL, EFFECT_TYPE_DOLBY_AUDIO_PROCESSING, i, i2);
        this.mProfileSupported = true;
        this.mParamListener = null;
        this.mBaseParamListener = null;
        this.mParamListenerLock = new Object();
        if (i2 == 0) {
            Log.i("DolbyAudioEffect", "Creating a DolbyAudioEffect to global output mix!");
        }
        this.mSessionId = i2;
        if (sNumOfProfiles == 0) {
            sNumOfProfiles = getNumOfProfiles();
        }
    }

    private static int[] byteArrayToInt32Array(byte[] bArr, int i, int i2) {
        int length = (bArr.length - i) >> 2;
        if (length <= i2) {
            i2 = length;
        }
        int[] iArr = new int[i2];
        for (int i3 = 0; i3 < i2; i3++) {
            int i4 = i3 * 4;
            iArr[i3] = (bArr[i4 + i] & 255) | ((bArr[(i4 + 3) + i] & 255) << 24) | ((bArr[(i4 + 2) + i] & 255) << 16) | ((bArr[(i4 + 1) + i] & 255) << 8);
        }
        return iArr;
    }

    private static int byteArrayToInt32(byte[] bArr) {
        return (bArr[0] & 255) | ((bArr[3] & 255) << 24) | ((bArr[2] & 255) << 16) | ((bArr[1] & 255) << 8);
    }

    private static int int32ToByteArray(int i, byte[] bArr, int i2) {
        int i3 = i2 + 1;
        bArr[i2] = (byte) (i & 255);
        int i4 = i3 + 1;
        bArr[i3] = (byte) ((i >>> 8) & 255);
        bArr[i4] = (byte) ((i >>> 16) & 255);
        bArr[i4 + 1] = (byte) ((i >>> 24) & 255);
        return 4;
    }

    private static int int32ArrayToByteArray(int[] iArr, byte[] bArr, int i) {
        int length = iArr.length;
        for (int i2 : iArr) {
            int i3 = i + 1;
            bArr[i] = (byte) ((i2 >>> 0) & 255);
            int i4 = i3 + 1;
            bArr[i3] = (byte) ((i2 >>> 8) & 255);
            int i5 = i4 + 1;
            bArr[i4] = (byte) ((i2 >>> 16) & 255);
            i = i5 + 1;
            bArr[i5] = (byte) ((i2 >>> 24) & 255);
        }
        return length << 2;
    }

    protected int setBoolParam(int i, boolean z) {
        byte[] bArr = new byte[12];
        int int32ToByteArray = int32ToByteArray(i, bArr, 0) + 0;
        int32ToByteArray(z ? 1 : 0, bArr, int32ToByteArray + int32ToByteArray(1, bArr, int32ToByteArray));
        checkReturnValue(setParameter(5, bArr));
        return 0;
    }

    protected int setIntParam(int i, int i2) {
        byte[] bArr = new byte[12];
        int int32ToByteArray = int32ToByteArray(i, bArr, 0) + 0;
        int32ToByteArray(i2, bArr, int32ToByteArray + int32ToByteArray(1, bArr, int32ToByteArray));
        checkReturnValue(setParameter(5, bArr));
        return 0;
    }

    protected boolean getBoolParam(int i) {
        byte[] bArr = new byte[12];
        int32ToByteArray(i, bArr, 0);
        checkReturnValue(getParameter(i + 5, bArr));
        return byteArrayToInt32(bArr) > 0;
    }

    protected int getIntParam(int i) {
        byte[] bArr = new byte[12];
        int32ToByteArray(i, bArr, 0);
        checkReturnValue(getParameter(i + 5, bArr));
        return byteArrayToInt32(bArr);
    }

    private void checkReturnValue(int i) {
        if (i < 0) {
            if (i == -5) {
                throw new UnsupportedOperationException("DolbyAudioEffect: invalid parameter operation");
            }
            if (i == -4) {
                throw new IllegalArgumentException("DolbyAudioEffect: bad parameter value");
            }
            throw new RuntimeException("DolbyAudioEffect: set/get parameter error");
        }
    }

    public void setDsOn(boolean z) {
        setBoolParam(0, z);
        super.setEnabled(z);
    }

    public boolean getDsOn() {
        return getBoolParam(0);
    }

    public String getDsVersion() {
        return "DAX3";
    }

    public void setDialogEnhancerEnabled(boolean z) {
        setDapParameter(DsParams.DialogEnhancementEnable.toInt(), new int[]{z ? 1 : 0});
    }

    public boolean getDialogEnhancerEnabled() {
        return getDapParameter(DsParams.DialogEnhancementEnable.toInt())[0] != 0;
    }

    public void setVolumeLevelerEnabled(boolean z) {
        setDapParameter(DsParams.DolbyVolumeLevelerEnable.toInt(), new int[]{z ? 1 : 0});
    }

    public void setDialogEnhancerAmount(int i) throws IllegalArgumentException {
        if (i < 0 || i > 16) {
            Log.e("DolbyAudioEffect", "ERROR in setDialogEnhancerAmount(): Invalid amount " + i);
            throw new IllegalArgumentException();
        }
        setDapParameter(DsParams.DialogEnhancementAmount.toInt(), new int[]{i});
    }

    public boolean getVolumeLevelerEnabled() {
        int[] enabled = getDapParameter(DsParams.DolbyVolumeLevelerEnable.toInt());
        return enabled[0] != 0;
    }

    public int getDialogEnhancerAmount() {
        return getDapParameter(DsParams.DialogEnhancementAmount.toInt())[0];
    }

    public void setSpeakerVirtualizerEnabled(boolean z) throws UnsupportedOperationException {
        if (isMonoSpeaker()) {
            throw new UnsupportedOperationException();
        }
        int[] iArr = {z ? 1 : 0};
        setDapParameter(DsParams.DolbyVirtualSpeakerVirtualizerControl.toInt(), iArr);
        setDapParameter(DsParams.DolbyHeadphoneVirtualizerControl.toInt(), iArr);
    }

    public boolean getSpeakerVirtualizerEnabled() {
        int[] enabled = getDapParameter(DsParams.DolbyVirtualSpeakerVirtualizerControl.toInt());
        return enabled[0] != 0;
    }

    public void setBassEnhancerEnabled(boolean z) {
        setDapParameter(DsParams.BassEnable.toInt(), new int[]{z ? 1 : 0});
    }

    public boolean getBassEnhancerEnabled() {
        int[] enabled = getDapParameter(DsParams.BassEnable.toInt());
        return enabled[0] != 0;
    }

    public void setIeqPreset(int i) throws IllegalArgumentException {
        if (i < 0 || i > 3) {
            Log.e("DolbyAudioEffect", "ERROR in setIeqPreset(): Invalid IEq preset index" + i);
            throw new IllegalArgumentException();
        }
        setDapParameter(DsParams.IntelligentEqualizerPreset.toInt(), new int[]{i});
    }

    public int getIeqPreset() {
        return getDapParameter(DsParams.IntelligentEqualizerPreset.toInt())[0];
    }

    public boolean isGeqEnabled(int i) throws IllegalArgumentException {
        if (i >= DsTuning.internal_speaker.toInt() && i <= DsTuning.usb.toInt()) {
            return getDapParameter(getProfile(), i, DsParams.GraphicEqualizerEnable.toInt())[0] != 0;
        }
        Log.e("DolbyAudioEffect", "ERROR in isGeqEnabled(): Invalid port " + i);
        throw new IllegalArgumentException();
    }

    public void setGeqBandGains(int[] iArr) throws IllegalArgumentException {
        if (iArr == null || iArr.length != 20) {
            Log.e("DolbyAudioEffect", "ERROR in setGeqBandGains(): Invalid GEq gains for 20 frequency bands!");
            throw new IllegalArgumentException();
        }
        setDapParameter(DsParams.GraphicEqualizerBandGains.toInt(), iArr);
    }

    public int[] getGeqBandGains() {
        return getDapParameter(DsParams.GraphicEqualizerBandGains.toInt());
    }

    public void setProfile(int i) throws IllegalArgumentException {
        if (i < 0 || i >= sNumOfProfiles) {
            Log.e("DolbyAudioEffect", "ERROR in setProfile(): Invalid profile index" + i);
            throw new IllegalArgumentException();
        }
        setIntParam(167772160, i);
    }

    public int getProfile() {
        return getIntParam(167772160);
    }

    public int getNumOfProfiles() {
        return getIntParam(50331648);
    }

    public boolean isMonoSpeaker() {
        return getBoolParam(218103808);
    }

    public void setDapParameter(int i, int[] iArr) throws UnsupportedOperationException, IllegalArgumentException {
        setDapParameter(getProfile(), i, iArr);
    }

    public void setDapParameter(int i, int i2, int[] iArr) throws UnsupportedOperationException, IllegalArgumentException {
        if (i < 0 || i >= sNumOfProfiles) {
            Log.e("DolbyAudioEffect", "ERROR in setProfileParameter(): Invalid profile index" + i);
            throw new IllegalArgumentException();
        }
        if (isMonoSpeaker() && i2 == DsParams.DolbyVirtualSpeakerVirtualizerControl.toInt()) {
            throw new UnsupportedOperationException();
        }
        int length = iArr.length;
        byte[] bArr = new byte[(length + 4) * 4];
        int int32ToByteArray = int32ToByteArray(16777216, bArr, 0) + 0;
        int int32ToByteArray2 = int32ToByteArray + int32ToByteArray(length + 1, bArr, int32ToByteArray);
        int int32ToByteArray3 = int32ToByteArray2 + int32ToByteArray(i, bArr, int32ToByteArray2);
        int32ArrayToByteArray(iArr, bArr, int32ToByteArray3 + int32ToByteArray(i2, bArr, int32ToByteArray3));
        checkReturnValue(setParameter(5, bArr));
    }

    public int[] getDapParameter(int i) throws IllegalArgumentException {
        return getDapParameter(getProfile(), i);
    }

    public int[] getDapParameter(int i, int i2) throws IllegalArgumentException {
        int i3;
        if (i < 0 || i >= sNumOfProfiles) {
            Log.e("DolbyAudioEffect", "ERROR in getDapParameter(): Invalid profile index" + i);
            throw new IllegalArgumentException();
        }
        try {
            i3 = DsParams.kParamToLen.get(Integer.valueOf(i2)).intValue();
        } catch (NullPointerException unused) {
            i3 = 1;
        }
        byte[] bArr = new byte[(i3 + 2) * 4];
        checkReturnValue(getParameter((i2 << 16) + 16777221 + (i << 8), bArr));
        return byteArrayToInt32Array(bArr, 0, i3);
    }

    public int[] getDapParameter(int i, int i2, int i3) throws IllegalArgumentException {
        int i4;
        if (i < 0 || i >= sNumOfProfiles) {
            Log.e("DolbyAudioEffect", "ERROR in getDapParameter(): Invalid profile index" + i);
            throw new IllegalArgumentException();
        }
        if (i2 < DsTuning.internal_speaker.toInt() || i2 > DsTuning.usb.toInt()) {
            Log.e("DolbyAudioEffect", "ERROR in getDapParameter(): Invalid port" + i2);
            throw new IllegalArgumentException();
        }
        try {
            i4 = DsParams.kParamToLen.get(Integer.valueOf(i3)).intValue();
        } catch (NullPointerException unused) {
            i4 = 1;
        }
        byte[] bArr = new byte[(i4 + 2) * 4];
        checkReturnValue(getParameter((i3 << 16) + 33554437 + ((i << 12) | (i2 << 8)), bArr));
        return byteArrayToInt32Array(bArr, 0, i4);
    }

    public boolean isProfileSpecificSettingsModified(int i) throws IllegalArgumentException {
        if (i < 0 || i >= sNumOfProfiles) {
            Log.e("DolbyAudioEffect", "ERROR in isProfileSpecificSettingsModified(): Invalid profile index" + i);
            throw new IllegalArgumentException();
        }
        byte[] bArr = new byte[12];
        checkReturnValue(getParameter((i << 16) + 184549381, bArr));
        return byteArrayToInt32(bArr) > 0;
    }

    public void resetProfileSpecificSettings(int i) throws IllegalArgumentException {
        if (i < 0 || i >= sNumOfProfiles) {
            Log.e("DolbyAudioEffect", "ERROR in resetProfileSpecificSettings(): Invalid profile index" + i);
            throw new IllegalArgumentException();
        }
        setIntParam(201326592, i);
    }

    private int getPortDeviceNameLength(int i) throws IllegalArgumentException {
        if (i < DsTuning.internal_speaker.toInt() || i > DsTuning.usb.toInt()) {
            Log.e("DolbyAudioEffect", "ERROR in getPortDeviceNameLength(): Invalid port" + i);
            throw new IllegalArgumentException();
        }
        byte[] bArr = new byte[4];
        checkReturnValue(getParameter((i << 16) + 2, bArr));
        return byteArrayToInt32(bArr);
    }

    public String[] getTuningDevicesList(int i) throws IllegalArgumentException, UnsupportedOperationException, UnsupportedEncodingException {
        if (i < DsTuning.internal_speaker.toInt() || i > DsTuning.usb.toInt()) {
            Log.e("DolbyAudioEffect", "ERROR in getTuningDevicesList(): Invalid port" + i);
            throw new IllegalArgumentException();
        }
        int portDeviceNameLength = getPortDeviceNameLength(i);
        byte[] bArr = new byte[((portDeviceNameLength + 4) >> 2) * 4];
        checkReturnValue(getParameter((i << 16) + 1, bArr));
        try {
            return new String(bArr, "UTF-8").substring(0, portDeviceNameLength - 1).split("\\s+");
        } catch (UnsupportedEncodingException e) {
            throw e;
        }
    }

    public String getSelectedTuningDevice(int i) throws IllegalArgumentException, UnsupportedEncodingException {
        if (i < DsTuning.internal_speaker.toInt() || i > DsTuning.usb.toInt()) {
            Log.e("DolbyAudioEffect", "ERROR in getSelectedTuningDevice(): Invalid port" + i);
            throw new IllegalArgumentException();
        }
        byte[] bArr = new byte[((getPortDeviceNameLength(i) + 4) >> 2) * 4];
        checkReturnValue(getParameter((i << 16) + 4, bArr));
        try {
            return new String(bArr, "UTF-8").split("\\s+")[0];
        } catch (UnsupportedEncodingException e) {
            throw e;
        }
    }

    public void setSelectedTuningDevice(int i, String str) throws IllegalArgumentException {
        if (i < DsTuning.internal_speaker.toInt() || i > DsTuning.usb.toInt()) {
            Log.e("DolbyAudioEffect", "ERROR in setSelectedTuningDevice(): Invalid port" + i);
            throw new IllegalArgumentException();
        }
        int length = str.length();
        byte[] bArr = new byte[length + 4];
        System.arraycopy(str.getBytes(), 0, bArr, int32ToByteArray(i, bArr, 0) + 0, length);
        checkReturnValue(setParameter(4, bArr));
    }
}
