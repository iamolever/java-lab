package org.ovr.javalab.fixmsg.util;

import com.lmax.nanofix.fields.MsgType;
import com.lmax.nanofix.outgoing.FixMessageBuilder;
import net.openhft.chronicle.bytes.Bytes;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

public class FixMessageUtilTest {
    private final static String TEST_COMP_ID = "testCompId";
    private final static Bytes testCompIdBytes = Bytes.from(TEST_COMP_ID);
    private final static String logonMsg = new FixMessageBuilder()
            .messageType(MsgType.LOGIN)
            .senderCompID("sender1")
            .targetCompID("target1")
            .msgSeqNum(1)
            .sendingTime(DateTime.now())
            .username("hello")
            .build().toFixString();
    private final static Bytes logonBytes = Bytes.from(logonMsg);
    private final static Bytes compIdReadBuffer = Bytes.elasticByteBuffer();

    @Test
    public void testCompIdInternator() {
        final String internCompId = FixMessageUtil.internCompId(testCompIdBytes);
        Assertions.assertEquals(TEST_COMP_ID, internCompId);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Fork(value = 1)
    @Warmup(iterations = 2, time = 5, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
    public void testBytes() {
        logonBytes.readPosition(2);
        compIdReadBuffer.clear();
        logonBytes.read(compIdReadBuffer, 7);
        final String internCompId = FixMessageUtil.internCompId(compIdReadBuffer);
        //Assertions.assertEquals(internCompId, "FIX.4.4");
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Fork(value = 1)
    @Warmup(iterations = 2, time = 5, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
    public void testCompIdInternatorThroughtput() {
        final String internCompId = FixMessageUtil.internCompId(testCompIdBytes);
    }

    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(new String[]{FixMessageUtilTest.class.getSimpleName()});
    }
}
