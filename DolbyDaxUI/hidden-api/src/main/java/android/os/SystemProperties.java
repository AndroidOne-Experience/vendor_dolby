package android.os;
/** Compile-only platform signature. Never package this class. */
public final class SystemProperties {
    public static String get(String key, String defaultValue) { throw new UnsupportedOperationException("Compile-only API"); }
}
