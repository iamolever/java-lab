package org.ovr.javalab.fixnio.event;

import com.lmax.disruptor.RingBuffer;
import net.openhft.chronicle.bytes.Bytes;
import org.ovr.javalab.fixmsg.FixMessage;
import org.ovr.javalab.fixmsg.FixMessageHeader;
import org.ovr.javalab.fixmsg.util.ByteUtil;
import org.ovr.javalab.fixmsg.util.FixMessageUtil;
import org.ovr.javalab.fixnio.stream.FixInStreamCallback;

public class InMessageEventProducer implements FixInStreamCallback {
    private final RingBuffer<InMessageEvent> ringBuffer;
    private long sequence;
    private InMessageEvent event;
    private FixMessage fixMessage;

    public InMessageEventProducer(RingBuffer<InMessageEvent> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    @Override
    public void onMessageBegin(final Bytes buffer, final long offset, final long length) {
        this.sequence = ringBuffer.next();
        this.event = ringBuffer.get(sequence);
        this.fixMessage = event.getFixMessage();
        this.event.getBytes().write(buffer, offset, length);
    }

    @Override
    public void onMessageEnd() {
        ringBuffer.publish(sequence);
    }

    @Override
    public StreamBehavior onField(int tagNum, Bytes buffer) {
        switch (tagNum) {
            case FixMessageHeader.MsgType :
                fixMessage.setMsgType(FixMessageUtil.internMsgTypeId(buffer));
                break;
            case FixMessageHeader.SenderCompID :
                fixMessage.setSenderCompId(FixMessageUtil.internCompId(buffer));
                break;
            case FixMessageHeader.TargetCompID :
                fixMessage.setTargetCompId(FixMessageUtil.internCompId(buffer));
                break;
            case FixMessageHeader.MsgSeqNum :
                fixMessage.setSeqNum(ByteUtil.readIntFromBuffer(buffer, 0, buffer.readRemaining()));
                break;
        }
        return tagNum == FixMessageHeader.SendingTime ? StreamBehavior.BREAK : StreamBehavior.CONTINUE;
    }

    @Override
    public AfterErrorBehavior onError(String errorDesc) {
        return null;
    }
}
