package android.os;
/** Compile-only platform signature; never packaged in the APK. */
public class HwParcel {
    public void writeInterfaceToken(String value) { throw new UnsupportedOperationException(); }
    public void writeStrongBinder(IHwBinder value) { throw new UnsupportedOperationException(); }
    public void writeInt32(int value) { throw new UnsupportedOperationException(); }
    public void writeInt64(long value) { throw new UnsupportedOperationException(); }
    public void verifySuccess() { throw new UnsupportedOperationException(); }
    public void releaseTemporaryStorage() { throw new UnsupportedOperationException(); }
    public void release() { throw new UnsupportedOperationException(); }
    public long readInt64() { throw new UnsupportedOperationException(); }
    public String readString() { throw new UnsupportedOperationException(); }
    public java.util.ArrayList<String> readStringVector() { throw new UnsupportedOperationException(); }
    public java.util.ArrayList<Byte> readInt8Vector() { throw new UnsupportedOperationException(); }
    public void enforceInterface(String descriptor) { throw new UnsupportedOperationException(); }
    public void writeStatus(int status) { throw new UnsupportedOperationException(); }
    public void send() { throw new UnsupportedOperationException(); }
    public void writeStringVector(java.util.ArrayList<String> values) { throw new UnsupportedOperationException(); }
    public void writeString(String value) { throw new UnsupportedOperationException(); }
    public NativeHandle readNativeHandle() { throw new UnsupportedOperationException(); }
    public void writeBuffer(HwBlob blob) { throw new UnsupportedOperationException(); }
}
