package org.ovr.javalab.fixmsg.util;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.lang.Maths;

public abstract class ByteUtil {
    private static final int ASCII_DIGIT_RANGE_BEGIN = 48;
    private static final int ASCII_DIGIT_RANGE_END= 57;

    public static boolean isAsciiDigit(final byte asciiCharacter) {
        return asciiCharacter >= ASCII_DIGIT_RANGE_BEGIN && asciiCharacter <= ASCII_DIGIT_RANGE_END;
    }

    public static int readIntFromArray(final byte[] array, final int offset, final int len) {
        int value = 0;
        int power = len - 1;
        for (int i = offset; power >= 0; i++, power--) {
            byte b = array[i];

            value += (b - ASCII_DIGIT_RANGE_BEGIN) * Maths.power10(power);
        }
        return value;
    }

    public static int readIntFromBuffer(final Bytes buffer, final long offset, final long len) {
        return readIntFromBuffer(buffer, (int) offset, (int) len);
    }

    public static int readIntFromBuffer(final Bytes buffer, final int offset, final int len) {
        int value = 0;
        int power = len - 1;
        for (int i = offset; power >= 0; i++, power--) {
            char c = buffer.charAt(i);

            value += (c - ASCII_DIGIT_RANGE_BEGIN) * Maths.power10(power);
        }
        return value;
    }
}
