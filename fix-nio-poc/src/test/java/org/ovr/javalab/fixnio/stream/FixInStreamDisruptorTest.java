package org.ovr.javalab.fixnio.stream;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.nanofix.fields.MsgType;
import com.lmax.nanofix.outgoing.FixMessageBuilder;
import net.openhft.chronicle.bytes.Bytes;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ovr.javalab.fixmsg.FixMessage;
import org.ovr.javalab.fixmsg.FixMessageHeader;
import org.ovr.javalab.fixmsg.util.ByteUtil;
import org.ovr.javalab.fixmsg.util.FixMessageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FixInStreamDisruptorTest {
    private final static Logger logger = LoggerFactory.getLogger(FixInStreamDisruptorTest.class);

    private List<String> messages = new ArrayList<>();

    @BeforeEach
    public void prepare() {
        messages.clear();
        for (int i = 1; i <= 5; i++) {
            final String logonMsg = new FixMessageBuilder()
                    .messageType(MsgType.LOGIN)
                    .senderCompID("sender" + i)
                    .targetCompID("target" + i)
                    .msgSeqNum(i)
                    .sendingTime(DateTime.now())
                    .username("hello_" + i)
                    .build().toFixString();
            messages.add(logonMsg);
        }
    }

    @Test
    public void testDisruptor() {
        final CountDownLatch countDownLatch = new CountDownLatch(messages.size());
        // Specify the size of the ring buffer, must be power of 2.
        int bufferSize = 1024;

        // Construct the Disruptor
        Disruptor<FixEvent> disruptor = new Disruptor<>(
                FixEvent::new, bufferSize, Executors.defaultThreadFactory(),
                ProducerType.SINGLE, new BlockingWaitStrategy());

        // Connect the handler
        disruptor.handleEventsWith((fixEvent, l, b) -> {
            countDownLatch.countDown();
            System.out.println(fixEvent);
        });

        // Start the Disruptor, starts all threads running
        disruptor.start();

        // Get the ring buffer from the Disruptor to be used for publishing.
        RingBuffer<FixEvent> ringBuffer = disruptor.getRingBuffer();

        final FixEventProducer fixEventProducer = new FixEventProducer(ringBuffer);
        final Bytes bytes = Bytes.elasticByteBuffer(256);
        final FixInputStreamHandler fixInStream = new FixInStreamSpliterator(bytes, fixEventProducer);
        final String message1 = messages.get(0);
        bytes.write(message1);

        final String message2 = messages.get(1);
        bytes.write(message2.subSequence(0, 10));
        fixInStream.onRead();
        bytes.write(message2.substring(10));
        fixInStream.onRead();

        for (int i = 2; i < messages.size(); i++) {
            bytes.write(messages.get(i));
        }
        fixInStream.onRead();
        try {
            countDownLatch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assertions.assertFalse(countDownLatch.getCount() > 0);
    }

    public static class FixEventProducer implements FixInStreamCallback {
        private final RingBuffer<FixInStreamDisruptorTest.FixEvent> ringBuffer;
        private long sequence;
        private FixInStreamDisruptorTest.FixEvent event;
        private FixMessage fixMessage;
        private final boolean clearBeforeFill;

        public FixEventProducer(RingBuffer<FixInStreamDisruptorTest.FixEvent> ringBuffer, final boolean clearBeforeFill) {
            this.ringBuffer = ringBuffer;
            this.clearBeforeFill = clearBeforeFill;
        }

        public FixEventProducer(RingBuffer<FixInStreamDisruptorTest.FixEvent> ringBuffer) {
            this.ringBuffer = ringBuffer;
            this.clearBeforeFill = false;
        }

        @Override
        public void onMessageBegin(final Bytes buffer, final long offset, final long length) {
            this.sequence = ringBuffer.next();
            this.event = ringBuffer.get(sequence);
            if (clearBeforeFill) {
                this.event.clear();
            }
            this.fixMessage = event.getFixMessage();
            this.fixMessage.getRawMessage().write(buffer, offset, length);
        }

        @Override
        public void onMessageEnd(final int headerLen) {
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

    public static class FixEvent {
        private FixMessage fixMessage = FixMessage.instance();

        FixMessage getFixMessage() {
            return fixMessage;
        }

        public void clear() {
            fixMessage.clear();
        }

        @Override
        public String toString() {
            return "FixEvent{" +
                    "fixMessage=" + fixMessage +
                    '}';
        }
    }
}
