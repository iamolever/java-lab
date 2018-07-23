package org.ovr.javalab.fixnio.common;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.Test;

public class ElasticByteBufferTest {
    @Test
    public void smokeTest() {
        final Bytes readBuffer = Bytes.elasticByteBuffer(3);
        readBuffer.write("abcdef1");
        readBuffer.clear();
        readBuffer.write("abcdef2");
        readBuffer.clear();
        readBuffer.write("abcdef3");
        readBuffer.clear();
        readBuffer.write("abcdef4");
        readBuffer.clear();
    }
}
