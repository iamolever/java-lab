package org.ovr.javalab.fixnio.stream;

import net.openhft.chronicle.bytes.Bytes;

public interface FixInputStream {
    Bytes getBuffer();
    void onRead();
}
