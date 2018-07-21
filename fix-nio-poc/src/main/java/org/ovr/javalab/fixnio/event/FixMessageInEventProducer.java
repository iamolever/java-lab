package org.ovr.javalab.fixnio.event;

import com.lmax.disruptor.RingBuffer;
import net.openhft.chronicle.bytes.Bytes;
import org.ovr.javalab.fixmsg.FixMessage;
import org.ovr.javalab.fixmsg.FixMessageHeader;
import org.ovr.javalab.fixmsg.util.ByteUtil;
import org.ovr.javalab.fixmsg.util.FixMessageUtil;
import org.ovr.javalab.fixnio.connection.FixConnectionContext;
import org.ovr.javalab.fixnio.stream.FixInStreamCallback;

public class FixMessageInEventProducer implements FixInStreamCallback {
    private final RingBuffer<FixMessageInEvent> ringBuffer;
    private long sequence;
    private FixMessageInEvent event;
    private FixMessage fixMessage;
    private FixConnectionContext context;

    public FixMessageInEventProducer(final FixConnectionContext context, final RingBuffer<FixMessageInEvent> ringBuffer) {
        this.context = context;
        this.ringBuffer = ringBuffer;
    }

    @Override
    public void onMessageBegin(final Bytes buffer, final long offset, final long length) {
        this.sequence = ringBuffer.next();
        this.event = ringBuffer.get(sequence);
        this.fixMessage = event.getFixMessage();
        this.fixMessage.getRawMessage().write(buffer, offset, length);
        this.event.setFixConnectionContext(context);
    }

    @Override
    public void onMessageEnd(final int headerLen) {
        this.fixMessage.setHeaderLength(headerLen);
        ringBuffer.publish(sequence);
    }

    @Override
    public StreamBehavior onField(final boolean isBodyField, final int tagNum, final Bytes value) {
        if (!isBodyField) {
            switch (tagNum) {
                case FixMessageHeader.MsgType:
                    fixMessage.setMsgType(FixMessageUtil.internMsgTypeId(value));
                    break;
                case FixMessageHeader.SenderCompID:
                    fixMessage.setSenderCompId(FixMessageUtil.internCompId(value));
                    break;
                case FixMessageHeader.TargetCompID:
                    fixMessage.setTargetCompId(FixMessageUtil.internCompId(value));
                    break;
                case FixMessageHeader.MsgSeqNum:
                    fixMessage.setSeqNum(ByteUtil.readIntFromBuffer(value, 0, value.readRemaining()));
                    break;
            }
            return StreamBehavior.CONTINUE;
        } else {
            return StreamBehavior.BREAK;
        }
    }

    @Override
    public AfterErrorBehavior onError(String errorDesc) {
        return null;
    }
}
