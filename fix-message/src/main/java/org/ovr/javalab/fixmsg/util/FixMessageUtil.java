package org.ovr.javalab.fixmsg.util;

//import net.openhft.lang.pool.StringInterner;
import net.openhft.chronicle.core.pool.StringInterner;

public abstract class FixMessageUtil {
    private static final byte SOH = 1;
    private static final byte ASCII_EQUALS = (byte)61;

    private final static StringInterner compIdInterner = new StringInterner(8096);

    public static boolean isAsciiEquals(final byte asciiCharacter) {
        return asciiCharacter == ASCII_EQUALS;
    }

    public static boolean isAsciiSOH(final byte asciiCharacter) {
        return asciiCharacter == SOH;
    }

    public static String internCompId(final CharSequence charSequence) {
        return compIdInterner.intern(charSequence);
    }

}
