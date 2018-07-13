package org.ovr.javalab.fixmsg.util;

import net.openhft.chronicle.bytes.Bytes;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

public class ByteUtilBenchmark {
    private static final String origValue = "146";
    private static final Bytes bytes = Bytes.from(origValue);
    private static final byte[] array = origValue.getBytes();

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Fork(value = 1)
    @Warmup(iterations = 2, time = 5, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
    public void testIntParserFromBytes() {
        final int val = ByteUtil.readIntFromBuffer(bytes, 0, origValue.length());
    }

    private static void doSomething(final int val) {

    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Fork(value = 1)
    @Warmup(iterations = 2, time = 5, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
    public void testIntParserFromArray() {
        doSomething(ByteUtil.readIntFromArray(array, 0, origValue.length()));
    }

    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(new String[]{ByteUtilBenchmark.class.getSimpleName()});
    }
}
