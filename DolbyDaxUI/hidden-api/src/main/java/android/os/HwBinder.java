package android.os;
/** Compile-only platform signature; never packaged in the APK. */
public abstract class HwBinder implements IHwBinder {
    public static IHwBinder getService(String descriptor, String instance) throws RemoteException { throw new UnsupportedOperationException(); }
    public static void enableInstrumentation() { throw new UnsupportedOperationException(); }
    public void transact(int code, HwParcel request, HwParcel reply, int flags) throws RemoteException { throw new UnsupportedOperationException(); }
    public abstract void onTransact(int code, HwParcel request, HwParcel reply, int flags) throws RemoteException;
}
