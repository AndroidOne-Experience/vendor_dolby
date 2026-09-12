package android.media.audiofx;
import java.util.UUID;
/** Compile-only platform signatures. The device supplies all implementations. */
public class AudioEffect {
    public static final UUID EFFECT_TYPE_NULL = null;
    public AudioEffect(UUID type, UUID uuid, int priority, int session) { throw new UnsupportedOperationException("Compile-only API"); }
    public int setParameter(int parameter, byte[] value) { throw new UnsupportedOperationException("Compile-only API"); }
    public int getParameter(int parameter, byte[] value) { throw new UnsupportedOperationException("Compile-only API"); }
    public int setEnabled(boolean enabled) { throw new UnsupportedOperationException("Compile-only API"); }
    public boolean getEnabled() { throw new UnsupportedOperationException("Compile-only API"); }
    public boolean hasControl() { throw new UnsupportedOperationException("Compile-only API"); }
    public void release() { throw new UnsupportedOperationException("Compile-only API"); }
    public interface OnParameterChangeListener { void onParameterChange(AudioEffect effect, int status, byte[] parameter, byte[] value); }
}
