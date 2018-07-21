package org.ovr.javalab.fixnio.stream;

import com.lmax.nanofix.fields.MsgType;
import com.lmax.nanofix.outgoing.FixMessageBuilder;
import net.openhft.chronicle.bytes.Bytes;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ovr.javalab.fixmsg.FixMessage;
import org.ovr.javalab.fixmsg.FixMessageHeader;
import org.ovr.javalab.fixmsg.FixVersion;
import org.ovr.javalab.fixmsg.util.ByteUtil;
import org.ovr.javalab.fixmsg.util.FixMessageEncoder;
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
            public void onMessageEnd(final int headerLen) {
                logger.debug("End of FIX message");
            }

            @Override
            public AfterErrorBehavior onError(String errorDesc) {
                return AfterErrorBehavior.FATAL;
            }

            @Override
            public StreamBehavior onField(final boolean isBodyField, int tagNum, Bytes buffer) {
                if (!isBodyField) {
                    logger.debug("\t{}={}", tagNum, buffer.toString());
                    return StreamBehavior.CONTINUE;
                } else {
                    return StreamBehavior.BREAK;
                }
            }
        };
        final Bytes bytes = Bytes.elasticByteBuffer(256);
        final FixInputStreamHandler fixInStream = new FixInStreamSpliterator(bytes, streamCallback);
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

    @Test
    public void testFixEncoderFromInputStream() {
        final FixMessage fixMessage = FixMessage.instance();
        final FixInStreamCallback streamCallback = new FixInStreamCallback() {

            @Override
            public void onMessageBegin(final Bytes buffer, final long offset, final long length) {
                fixMessage.getRawMessage().write(buffer, offset, length);
            }

            @Override
            public void onMessageEnd(final int headerLen) {
                fixMessage.setHeaderLength(headerLen);
            }

            @Override
            public StreamBehavior onField(final boolean isBodyField, final int tagNum, final Bytes value) {
                if (!isBodyField) {
                    switch (tagNum) {
                        case FixMessageHeader.MsgType:
                            fixMessage.setMsgType(FixMessageUtil.internMsgTypeId(value));
                            break;
                        case FixMessageHeader.SenderCompID:
                            fixMessage.setSenderCompId(FixMessageUtil.internCompId(value));
                            break;
                        case FixMessageHeader.TargetCompID:
                            fixMessage.setTargetCompId(FixMessageUtil.internCompId(value));
                            break;
                        case FixMessageHeader.MsgSeqNum:
                            fixMessage.setSeqNum(ByteUtil.readIntFromBuffer(value, 0, value.readRemaining()));
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
        };
        final Bytes inBuffer = Bytes.elasticByteBuffer(256);
        final FixInputStreamHandler fixInStream = new FixInStreamSpliterator(inBuffer, streamCallback);
        final String message1 = messages.get(0);
        inBuffer.write(message1);
        fixInStream.onRead();

        final Bytes outBuffer = Bytes.elasticByteBuffer(256);
        FixMessageEncoder.stdEncoder(FixVersion.FIX44).encodeMessage(outBuffer, fixMessage);
        System.out.println(outBuffer);
    }
}
