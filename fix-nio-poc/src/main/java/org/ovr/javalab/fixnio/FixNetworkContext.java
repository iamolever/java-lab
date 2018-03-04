package org.ovr.javalab.fixnio;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class FixNetworkContext {
    final SocketChannel channel;
    final ByteBuffer readBuffer = ByteBuffer.allocate(256);
    final ByteBuffer writeBuffer = ByteBuffer.allocate(256);

    FixNetworkContext(SocketChannel channel) {
        this.channel = channel;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public ByteBuffer getReadBuffer() {
        return readBuffer;
    }

    public ByteBuffer getWriteBuffer() {
        return writeBuffer;
    }
}
