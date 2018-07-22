package org.ovr.javalab.fixnio.connection;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.IOTools;
import org.ovr.javalab.fixnio.common.FixMessageCircularQueue;
import org.ovr.javalab.fixnio.session.AcceptorFixSession;
import org.ovr.javalab.fixnio.session.FixSession;
import org.ovr.javalab.fixnio.stream.FixInStreamCallback;
import org.ovr.javalab.fixnio.stream.FixInStreamSpliterator;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class FixConnectionContext {
    private final SocketChannel channel;
    private final Bytes readBuffer = Bytes.elasticByteBuffer();
    private final Bytes writeBuffer = Bytes.elasticByteBuffer();
    private FixInStreamSpliterator inStreamHandler;
    private FixMessageCircularQueue outgoingQueue;
    private FixSession fixSession;

    public FixConnectionContext(final SocketChannel channel) {
        this.channel = channel;
        this.fixSession = new AcceptorFixSession(this);
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public ByteBuffer getInByteBuffer() {
        return (ByteBuffer) readBuffer.underlyingObject();
    }

    public ByteBuffer getOutByteBuffer() {
        return (ByteBuffer) writeBuffer.underlyingObject();
    }

    public Bytes getReadBuffer() {
        return readBuffer;
    }

    public Bytes getWriteBuffer() {
        return writeBuffer;
    }

    public FixInStreamSpliterator getInStreamHandler() {
        return inStreamHandler;
    }

    public void handleInStreamWith(final FixInStreamCallback inStreamHandler) {
        this.inStreamHandler = new FixInStreamSpliterator(this.readBuffer, inStreamHandler);
    }

    public void initOutgoingQueue(final int capacity) {
        this.outgoingQueue = new FixMessageCircularQueue(capacity);
    }

    public FixMessageCircularQueue getOutgoingQueue() {
        return outgoingQueue;
    }

    public FixSession getFixSession() {
        return fixSession;
    }

    public void setFixSession(final FixSession fixSession) {
        this.fixSession = fixSession;
    }

    public void close() {
        IOTools.clean(this.getInByteBuffer());
        IOTools.clean(this.getOutByteBuffer());
    }

    @Override
    public String toString() {
        return "FixConnectionContext{" +
                "channel=" + channel +
                '}';
    }
}
