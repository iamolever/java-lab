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
        this.event.getBytes().write(buffer, offset, length);
        this.event.setFixConnectionContext(context);
    }

    @Override
    public void onMessageEnd() {
        ringBuffer.publish(sequence);
    }

    @Override
    public StreamBehavior onField(int tagNum, Bytes buffer) {
        if (FixMessageUtil.isHeaderField(tagNum)) {
            switch (tagNum) {
                case FixMessageHeader.MsgType:
                    fixMessage.setMsgType(FixMessageUtil.internMsgTypeId(buffer));
                    break;
                case FixMessageHeader.SenderCompID:
                    fixMessage.setSenderCompId(FixMessageUtil.internCompId(buffer));
                    break;
                case FixMessageHeader.TargetCompID:
                    fixMessage.setTargetCompId(FixMessageUtil.internCompId(buffer));
                    break;
                case FixMessageHeader.MsgSeqNum:
                    fixMessage.setSeqNum(ByteUtil.readIntFromBuffer(buffer, 0, buffer.readRemaining()));
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
