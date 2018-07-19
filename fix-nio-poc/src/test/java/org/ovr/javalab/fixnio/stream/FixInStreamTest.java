package org.ovr.javalab.fixnio.stream;

import com.lmax.nanofix.fields.MsgType;
import com.lmax.nanofix.outgoing.FixMessageBuilder;
import net.openhft.chronicle.bytes.Bytes;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ovr.javalab.fixmsg.FixMessageHeader;
import org.ovr.javalab.fixmsg.util.FixMessageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class FixInStreamTest {
    private final static Logger logger = LoggerFactory.getLogger(FixInStreamTest.class);

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
                    .username("user_" + i)
                    .build().toFixString();
            messages.add(logonMsg);
        }
    }

    @Test
    public void testFixInStream() {
        final FixInStreamCallback streamCallback = new FixInStreamCallback() {
            @Override
            public void onMessageBegin(final Bytes buffer, final long offset, final long length) {
                Bytes message = Bytes.allocateDirect(length);
                message.write(buffer, 0, length);

                logger.debug("Begin of FIX message: {}", message);
            }

            @Override
            public void onMessageEnd() {
                logger.debug("End of FIX message");
            }

            @Override
            public AfterErrorBehavior onError(String errorDesc) {
                return AfterErrorBehavior.FATAL;
            }

            @Override
            public StreamBehavior onField(int tagNum, Bytes buffer) {
                /*logger.debug("\t{}={}", tagNum, buffer.toString());
                return StreamBehavior.CONTINUE;*/
                if (FixMessageUtil.isHeaderField(tagNum)) {
                    logger.debug("\t{}={}", tagNum, buffer.toString());
                    return StreamBehavior.CONTINUE;
                } else {
                    return StreamBehavior.BREAK;
                }
            }
        };
        final Bytes bytes = Bytes.elasticByteBuffer(256);
        //final StatefulFixInStream fixInStream = new StatefulFixInStream(bytes, streamCallback, 256);
        final FixInputStream fixInStream = new FixInStreamSpliterator(bytes, streamCallback);
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
