package org.ovr.javalab.fixnio.stream;

import com.lmax.nanofix.fields.MsgType;
import com.lmax.nanofix.outgoing.FixMessageBuilder;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.lang.collection.HugeCollections;
import net.openhft.lang.collection.HugeQueue;
import org.joda.time.DateTime;
import org.openjdk.jmh.annotations.*;
import org.ovr.javalab.fixmsg.FixMessage;
import org.ovr.javalab.fixmsg.FixMessageHeader;
import org.ovr.javalab.fixmsg.util.ByteUtil;
import org.ovr.javalab.fixmsg.util.FixMessageUtil;

import java.util.concurrent.TimeUnit;

public class FixInStreamBenchmark {
    private final static String logonMsg = new FixMessageBuilder()
            .messageType(MsgType.LOGIN)
            .senderCompID("sender1")
            .targetCompID("target1")
            .msgSeqNum(1)
            .sendingTime(DateTime.now())
            .username("hello1")
            .build().toFixString();
    private final static HugeQueue<FixMessage> queue = HugeCollections.newQueue(FixMessage.class, 100*1000L);

    private final static FixInStreamCallback streamCallback = new FixInStreamCallback() {
        private FixMessage fixMessage = queue.offer();

        @Override
        public void onMessageBegin(final Bytes buffer, final long offset, final long length) {
            fixMessage = queue.offer();
            /*Bytes message = fixMessage.getRawMessage();
            if (fixMessage != null) {
                message.clear();
            } else {
                message = Bytes.allocateDirect(length);
                fixMessage.setRawMessage(message);
            }*/
            Bytes message = Bytes.allocateDirect(length);
            message.write(buffer, 0, length);
        }

        @Override
        public void onMessageEnd() {

        }

        @Override
        public AfterErrorBehavior onError(final String errorDesc) {
            return AfterErrorBehavior.FATAL;
        }

        @Override
        public StreamBehavior onField(final int tagNum, final Bytes buffer) {
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
    };

    private final static long bufferSize = 256*1000;
    private final static Bytes bytes = Bytes.allocateElasticDirect((int)bufferSize);
    private final static FixInputStream fixInStream = new FixInStreamSpliterator(bytes, streamCallback);

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
        final FixMessage fixMessage = queue.take();
        //System.out.println(fixMessage.getSenderCompId());
    }

    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(new String[]{FixInStreamBenchmark.class.getSimpleName()});
    }
}
