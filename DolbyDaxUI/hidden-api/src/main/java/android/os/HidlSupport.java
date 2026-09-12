package android.os;
/** Compile-only platform signature; never packaged in the APK. */
public final class HidlSupport {
    public static boolean interfacesEqual(IHwInterface a, Object b) { throw new UnsupportedOperationException(); }
    public static int getPidIfSharable() { throw new UnsupportedOperationException(); }
}
