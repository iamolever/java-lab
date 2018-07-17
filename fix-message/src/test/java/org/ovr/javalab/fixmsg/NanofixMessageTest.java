package org.ovr.javalab.fixmsg;

import com.lmax.nanofix.fields.MsgType;
import com.lmax.nanofix.outgoing.FixMessage;
import com.lmax.nanofix.outgoing.FixMessageBuilder;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class NanofixMessageTest {
    @Test
    public void testLogonString() {
        final FixMessage fixMessage = new FixMessageBuilder()
                .messageType(MsgType.LOGIN)
                .senderCompID("sender1")
                .targetCompID("target1")
                .msgSeqNum(1)
                .sendingTime(DateTime.now())
                .username("hello1")
                .build();

        System.out.println(fixMessage.toFixString());
    }
}
