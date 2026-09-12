package android.os;
/** Compile-only platform signature; never packaged in the APK. */
public final class ServiceManager {
    public static IBinder checkService(String name) { throw new UnsupportedOperationException(); }
}
