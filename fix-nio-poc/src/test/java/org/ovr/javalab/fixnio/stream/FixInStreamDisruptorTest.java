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

import java.nio.ByteBuffer;
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
        final FixInputStream fixInStream = new FixInStreamSpliterator(bytes, fixEventProducer);
        final String message1 = messages.get(0);
        bytes.write(message1);
        //fixInStream.onRead();

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

        public FixEventProducer(RingBuffer<FixInStreamDisruptorTest.FixEvent> ringBuffer) {
            this.ringBuffer = ringBuffer;
        }

        @Override
        public void onMessageBegin(Bytes buffer, long offset, long length) {
            this.sequence = ringBuffer.next();
            this.event = ringBuffer.get(sequence);
            this.fixMessage = event.getFixMessage();
            //this.event.getBytes().clear();
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


    public static class FixEvent {
        private Bytes bytes = Bytes.allocateElasticDirect(512);
        private FixMessage fixMessage = FixMessage.instance();

        public Bytes getBytes() {
            return bytes;
        }

        public FixMessage getFixMessage() {
            return fixMessage;
        }

        @Override
        public String toString() {
            return "FixEvent{" +
                    "bytes=" + bytes +
                    ", fixMessage=" + fixMessage +
                    '}';
        }

        public void clear() {
            bytes.clear();
        }
    }
}
