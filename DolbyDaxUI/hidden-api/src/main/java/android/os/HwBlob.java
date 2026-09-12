package android.os;
/** Compile-only platform signature; never packaged in the APK. */
public class HwBlob {
    public HwBlob(int size) { throw new UnsupportedOperationException(); }
    public void putInt32(long offset, int value) { throw new UnsupportedOperationException(); }
    public void putInt64(long offset, long value) { throw new UnsupportedOperationException(); }
    public void putBool(long offset, boolean value) { throw new UnsupportedOperationException(); }
    public void putInt8Array(long offset, byte[] value) { throw new UnsupportedOperationException(); }
    public void putBlob(long offset, HwBlob value) { throw new UnsupportedOperationException(); }
}
