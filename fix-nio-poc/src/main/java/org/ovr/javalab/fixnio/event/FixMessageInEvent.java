package org.ovr.javalab.fixnio.event;

import net.openhft.chronicle.bytes.Bytes;
import org.ovr.javalab.fixmsg.FixMessage;
import org.ovr.javalab.fixnio.connection.FixConnectionContext;

public class FixMessageInEvent {
    private final static int DEFAULT_BUFFER_SIZE = 512;

    private Bytes bytes = Bytes.allocateElasticDirect(DEFAULT_BUFFER_SIZE);
    private FixMessage fixMessage = FixMessage.instance();
    private FixConnectionContext fixConnectionContext;

    public Bytes getBytes() {
        return bytes;
    }

    public FixMessage getFixMessage() {
        return fixMessage;
    }

    public FixConnectionContext getFixConnectionContext() {
        return fixConnectionContext;
    }

    public void setFixConnectionContext(final FixConnectionContext fixConnectionContext) {
        this.fixConnectionContext = fixConnectionContext;
    }

    public void clear() {
        this.bytes.clear();
        this.fixConnectionContext = null;
        this.fixMessage.clear();
    }

    @Override
    public String toString() {
        return "FixMessageInEvent{" +
                "bytes=" + bytes.toDebugString() +
                ", fixMessage=" + fixMessage +
                ", fixConnectionContext=" + fixConnectionContext +
                '}';
    }
}
