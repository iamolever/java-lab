package org.ovr.javalab.fixnio.stream;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.nanofix.fields.MsgType;
import com.lmax.nanofix.outgoing.FixMessageBuilder;
import net.openhft.chronicle.bytes.Bytes;
import org.joda.time.DateTime;
import org.openjdk.jmh.annotations.*;
import org.ovr.javalab.fixmsg.FixMessage;
import org.ovr.javalab.fixmsg.FixMessageHeader;
import org.ovr.javalab.fixmsg.util.ByteUtil;
import org.ovr.javalab.fixmsg.util.FixMessageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FixInStreamToDisruptorBenchmark {
    private final static Logger logger = LoggerFactory.getLogger(FixInStreamToDisruptorBenchmark.class);
    private final static String logonMsg = new FixMessageBuilder()
            .messageType(MsgType.LOGIN)
            .senderCompID("sender1")
            .targetCompID("target1")
            .msgSeqNum(1)
            .sendingTime(DateTime.now())
            .username("user1")
            .build().toFixString();

    private final static long bufferSize = 128*1000;
    private final static int ringBufferSize = 1024*64;
    private final static Bytes bytes = Bytes.allocateElasticDirect((int)bufferSize);

    private final static Disruptor<FixInStreamDisruptorTest.FixEvent> disruptor = new Disruptor<>(
            FixInStreamDisruptorTest.FixEvent::new, ringBufferSize, Executors.defaultThreadFactory(),
            ProducerType.SINGLE, new BlockingWaitStrategy());
    private final static RingBuffer<FixInStreamDisruptorTest.FixEvent> ringBuffer = disruptor.getRingBuffer();
    private final static FixEventProducer producer = new FixEventProducer(ringBuffer);
    private final static FixInputStream fixInStream = new FixInStreamSpliterator(bytes, producer);

    @Benchmark
    /*@BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Fork(value = 1)
    @Warmup(iterations = 2, time = 10, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)*/
    //@Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Fork(value = 1)
    @Warmup(iterations = 2, time = 10, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)

    public void testStreamLatency() {
        bytes.write(logonMsg);
        fixInStream.onRead();

    }

    private static void runDisruptor() {
        // Connect the handler
        disruptor.handleEventsWith(new FixEventHandler());

        // Start the Disruptor, starts all threads running
        disruptor.start();
        //disruptor.
    }

    public static class FixEventHandler implements EventHandler<FixInStreamDisruptorTest.FixEvent> {
        public void onEvent(FixInStreamDisruptorTest.FixEvent event, long sequence, boolean endOfBatch) {
            logger.debug("Event: " + event);
        }
    }

    public static class FixEventFactory implements EventFactory<FixInStreamDisruptorTest.FixEvent> {
        public FixInStreamDisruptorTest.FixEvent newInstance() {
            return new FixInStreamDisruptorTest.FixEvent();
        }
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
            this.event.getBytes().clear();
            this.event.getBytes().write(buffer, offset, length);
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

    public static void main(String[] args) throws Exception {
        runDisruptor();
        org.openjdk.jmh.Main.main(new String[]{FixInStreamToDisruptorBenchmark.class.getSimpleName()});
    }
}
