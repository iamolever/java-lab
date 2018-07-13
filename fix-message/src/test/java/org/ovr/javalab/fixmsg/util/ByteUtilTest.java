package org.ovr.javalab.fixmsg.util;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ByteUtilTest {

    @Test
    public void testReadIntFromArray() {
        final String origValue = "146";
        final byte[] buffer = origValue.getBytes();
        final int value = ByteUtil.readIntFromArray(buffer,0, buffer.length);
        Assertions.assertEquals(value, Integer.parseInt(origValue));
    }

    @Test
    public void testReadIntFromBytes() {
        final String origValue = "146";
        final Bytes buffer = Bytes.from(origValue);
        final int value = ByteUtil.readIntFromBuffer(buffer,0, origValue.length());
        Assertions.assertEquals(value, Integer.parseInt(origValue));
    }
}
