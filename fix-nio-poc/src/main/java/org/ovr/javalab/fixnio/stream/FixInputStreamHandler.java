package org.ovr.javalab.fixnio.stream;

import net.openhft.chronicle.bytes.Bytes;

public interface FixInputStreamHandler {
    Bytes getBuffer();
    void onRead();
}
