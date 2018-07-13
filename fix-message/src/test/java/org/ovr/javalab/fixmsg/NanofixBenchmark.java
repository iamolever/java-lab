package org.ovr.javalab.fixmsg;

import com.lmax.nanofix.byteoperations.ByteUtil;
/*import com.lmax.nanofix.fields.MsgType;
import com.lmax.nanofix.outgoing.FixMessage;
import com.lmax.nanofix.outgoing.FixMessageBuilder;*/
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

public class NanofixBenchmark {
    private static byte[] bytes = "135".getBytes();

    /*@Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Fork(value = 1)
    @Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
    public void testLogonBuilder() {
        final FixMessage fixMessage = new FixMessageBuilder().messageType(MsgType.LOGIN).username("hello").build();
    }*/

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Fork(value = 1)
    @Warmup(iterations = 2, time = 5, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
    public void testIntParser() {
        final int val = ByteUtil.readIntFromAscii(bytes, 0, bytes.length);
    }

    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(new String[]{NanofixBenchmark.class.getSimpleName()});
    }
}
