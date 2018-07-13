package org.ovr.javalab.stream;

import com.lmax.nanofix.fields.MsgType;
import com.lmax.nanofix.outgoing.FixMessageBuilder;
import net.openhft.chronicle.bytes.Bytes;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class StatefulFixInStreamTest {
    private final Logger logger = LoggerFactory.getLogger(StatefulFixInStreamTest.class);

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
    public void testFixInStream() {
        final FixInStreamCallback streamCallback = new FixInStreamCallback() {
            @Override
            public void onMessageBegin() {
                logger.debug("Begin of FIX message");
            }

            @Override
            public void onMessageEnd() {
                logger.debug("End of FIX message");
            }

            @Override
            public AfterErrorBehavior onError(String errorDesc) {
                return AfterErrorBehavior.BREAK;
            }

            @Override
            public StreamBehavior onTagValue(int tagNum, Bytes buffer) {
                logger.debug("\t{}={}", tagNum, buffer.toString());
                return StreamBehavior.CONTINUE;
            }
        };
        final Bytes bytes = Bytes.elasticByteBuffer(256);
        final StatefulFixInStream fixInStream = new StatefulFixInStream(bytes, streamCallback, 256);
        final String message1 = messages.get(0);
        bytes.write(message1);
        fixInStream.onRead();

        final String message2 = messages.get(1);
        bytes.write(message2.subSequence(0, 10));
        fixInStream.onRead();
        bytes.write(message2.substring(10));
        fixInStream.onRead();

        for (int i = 2; i < messages.size(); i++) {
            bytes.write(messages.get(i));
        }
        fixInStream.onRead();
    }
}
