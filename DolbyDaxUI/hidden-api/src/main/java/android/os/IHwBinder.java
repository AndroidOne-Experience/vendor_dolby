package android.os;
/** Compile-only platform signature; never packaged in the APK. */
public interface IHwBinder {
    interface DeathRecipient { void serviceDied(long cookie); }
    IHwInterface queryLocalInterface(String descriptor);
    void transact(int code, HwParcel request, HwParcel reply, int flags) throws RemoteException;
    boolean linkToDeath(DeathRecipient recipient, long cookie) throws RemoteException;
    boolean unlinkToDeath(DeathRecipient recipient) throws RemoteException;
}
