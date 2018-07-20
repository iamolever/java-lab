package org.ovr.javalab.fixnio.stream;

import net.openhft.chronicle.bytes.Bytes;
import org.ovr.javalab.fixmsg.util.ByteUtil;
import org.ovr.javalab.fixmsg.util.FixMessageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FixInStreamSpliterator implements FixInputStreamHandler {
    private final Logger logger = LoggerFactory.getLogger(FixInStreamSpliterator.class);

    private final Bytes buffer;
    private final Bytes fixBeginPattern = getFixBeginTagPattern();;
    private final Bytes fixChkSumPattern = getFixChkSumTagPattern();
    private final Bytes value = Bytes.elasticByteBuffer();
    private FixInStreamCallback callback;

    private int offset = 0;
    private int tagNum;

    public FixInStreamSpliterator(final Bytes buffer, final FixInStreamCallback callback) {
        this.buffer = buffer;
        this.callback = callback;
    }

    private static Bytes getFixBeginTagPattern() {
        final Bytes pattern = Bytes.elasticByteBuffer(4);
        pattern.writeByte(FixMessageUtil.SOH);
        pattern.write("8=");
        return pattern;
    }

    private static Bytes getFixChkSumTagPattern() {
        final Bytes pattern = Bytes.elasticByteBuffer(4);
        pattern.writeByte(FixMessageUtil.SOH);
        pattern.write("10=");
        return pattern;
    }

    @Override
    public Bytes getBuffer() {
        return buffer;
    }

    @Override
    public void onRead() {
        while (buffer.readRemaining() > 0) {
            final long nextMsgOffset = buffer.indexOf(fixBeginPattern);
            if (nextMsgOffset > 0) {
                onReadFixMessage(nextMsgOffset + 1);
            } else {
                final long chkSumIdx = buffer.indexOf(fixChkSumPattern);
                if (chkSumIdx > 0) {
                    final long endOfMsgIdx = chkSumIdx + 7;
                    onReadFixMessage(endOfMsgIdx);
                    buffer.clear();
                } else {
                    buffer.compact();
                    break;
                }
            }
        }
    }

    private void onReadFixMessage(final long nextMsgOffset) {
        doFieldIterator(nextMsgOffset);
    }

    private void doFieldIterator(final long nextMsgOffset) {
        final long stopReadPosition = buffer.readPosition() + nextMsgOffset;
        callback.onMessageBegin(buffer, buffer.readPosition(), nextMsgOffset);
        while (buffer.readPosition() < stopReadPosition) {
            value.clear();
            doTag();
            doValue();
            final FixInStreamCallback.StreamBehavior state = callback.onField(tagNum, value);
            if (state == FixInStreamCallback.StreamBehavior.BREAK) {
                buffer.readPosition(stopReadPosition);
                break;
            }
        }
        callback.onMessageEnd();
    }

    private void doTag() {
        while (!FixMessageUtil.isAsciiEquals(buffer.readByte(buffer.readPosition() + offset))) {
            offset++;
        }
        tagNum = ByteUtil.readIntFromBuffer(buffer, 0, offset);
        buffer.readSkip(offset + 1);
        offset = 0;
    }

    private void doValue() {
        while (!FixMessageUtil.isAsciiSOH(buffer.readByte(buffer.readPosition() + offset))) {
            offset++;
        }
        buffer.read(value, offset);
        buffer.readSkip(1);
        offset = 0;
    }
}