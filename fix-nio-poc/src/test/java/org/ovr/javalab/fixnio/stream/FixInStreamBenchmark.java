package org.ovr.javalab.fixnio.stream;

import com.lmax.nanofix.fields.MsgType;
import com.lmax.nanofix.outgoing.FixMessageBuilder;
import net.openhft.chronicle.bytes.Bytes;
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

    private final static FixInStreamCallback streamCallback = new FixInStreamCallback() {
        private FixMessage fixMessage = FixMessage.instance();

        @Override
        public void onMessageBegin(final Bytes buffer, final long offset, final long length) {

        }

        @Override
        public void onMessageEnd(final int headerLen) {
            fixMessage.clear();
        }

        @Override
        public AfterErrorBehavior onError(final String errorDesc) {
            return AfterErrorBehavior.FATAL;
        }

        @Override
        public StreamBehavior onField(final boolean isBodyField, final int tagNum, final Bytes buffer) {
            if (!isBodyField) {
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
    };

    private final static long bufferSize = 256*1000;
    private final static Bytes bytes = Bytes.allocateElasticDirect((int)bufferSize);
    private final static FixInputStreamHandler fixInStream = new FixInStreamSpliterator(bytes, streamCallback);

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

    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(new String[]{FixInStreamBenchmark.class.getSimpleName()});
    }
}
