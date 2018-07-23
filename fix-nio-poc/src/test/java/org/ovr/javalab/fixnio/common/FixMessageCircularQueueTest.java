package org.ovr.javalab.fixnio.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ovr.javalab.fixmsg.FixMessage;

public class FixMessageCircularQueueTest {

    @Test
    public void smokeTest() throws Exception {
        final int size = 100;
        final FixMessageCircularQueue queue = new FixMessageCircularQueue(size);
        Assertions.assertEquals(size, queue.capacity());
        Assertions.assertEquals(0, queue.size());
        Assertions.assertEquals(size, queue.available());
        final FixMessage fixMessage1 = queue.getToPublish();
        fixMessage1.setSeqNum(100);
        queue.publish();
        Assertions.assertEquals(1, queue.size());
        Assertions.assertEquals(99, queue.available());
        final FixMessage fixMessage2 = queue.getToPublish();
        fixMessage2.setSeqNum(101);
        queue.publish();
        Assertions.assertEquals(2, queue.size());
        Assertions.assertEquals(98, queue.available());
        final FixMessage fixMessage3 = queue.remove();
        Assertions.assertEquals(100, fixMessage3.getSeqNum());
        fixMessage3.clear();
        Assertions.assertEquals(1, queue.size());
        Assertions.assertEquals(99, queue.available());
        final FixMessage fixMessage4 = queue.remove();
        Assertions.assertEquals(101, fixMessage4.getSeqNum());
        fixMessage4.clear();
        Assertions.assertEquals(0, queue.size());
        Assertions.assertEquals(size, queue.available());
    }
}
