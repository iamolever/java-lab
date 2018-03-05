package org.ovr.javalab.fixnio;

import net.openhft.chronicle.bytes.Bytes;
import java.nio.channels.SocketChannel;

public class FixConnectionContext {
    final SocketChannel channel;
    final Bytes readBuffer = Bytes.elasticByteBuffer();
    final Bytes writeBuffer = Bytes.elasticByteBuffer();

    FixConnectionContext(SocketChannel channel) {
        this.channel = channel;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public Bytes getReadBuffer() {
        return readBuffer;
    }

    public Bytes getWriteBuffer() {
        return writeBuffer;
    }
}
