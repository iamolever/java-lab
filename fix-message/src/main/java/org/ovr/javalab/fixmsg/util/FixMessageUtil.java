package org.ovr.javalab.fixmsg.util;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.StringInterner;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class FixMessageUtil {
    public static final byte SOH = 1;
    public static final byte ASCII_EQUALS = (byte)61;
    public static final int CHKSUM_FILES_LEN = 7;

    private static final List<Integer> stdMessageHeaderFields = Arrays.asList(
        8,9,35,49,56,115,128,90,91,34,50,142,57,143,116,144,129,145,43,97,52,122,212,213,347,369,627,628,629,630
    );
    private static final boolean[] stdMessageHeaderFieldsCache = new boolean[Collections.max(stdMessageHeaderFields) + 1];

    private final static StringInterner compIdInterner = new StringInterner(8096);
    private final static StringInterner msgTypeInternet = new StringInterner(256);

    static {
        calcHeaderFieldCache();
    }


    public static boolean isAsciiEquals(final byte asciiCharacter) {
        return asciiCharacter == ASCII_EQUALS;
    }

    public static boolean isAsciiSOH(final byte asciiCharacter) {
        return asciiCharacter == SOH;
    }

    public static String internCompId(final CharSequence charSequence) {
        return compIdInterner.intern(charSequence);
    }

    public static String internMsgTypeId(final CharSequence charSequence) {
        return msgTypeInternet.intern(charSequence);
    }

    public static boolean isHeaderField(final int tagNum) {
        return stdMessageHeaderFieldsCache[tagNum];
    }

    private static void calcHeaderFieldCache() {
        stdMessageHeaderFields.forEach(tag -> stdMessageHeaderFieldsCache[tag] = true);
    }

    public static int getChecksum(final Bytes bytes, final int begin, final int end) {
        return bytes.byteCheckSum(begin, end);
    }

    public static OffsetDateTime getNowAsUTC() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    public static boolean isAdminMsg(final String type) {
        return type.length() == 1 && isAdminMsg(type.charAt(0));
    }

    public static boolean isAdminMsg(final char type) {
        return (type >= '0' && type <= '5') || type == 'A';
    }
}
