package org.ovr.javalab.fixnio.stream;

import net.openhft.chronicle.bytes.Bytes;
import org.ovr.javalab.fixmsg.util.ByteUtil;
import org.ovr.javalab.fixmsg.util.FixMessageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ovr.javalab.fixmsg.util.FixMessageUtil.CHKSUM_FILES_LEN;

public class FixInStreamSpliterator implements FixInputStreamHandler {
    private final Logger logger = LoggerFactory.getLogger(FixInStreamSpliterator.class);

    private final Bytes buffer;
    private final Bytes fixBeginPattern = getFixBeginTagPattern();;
    private final Bytes fixChkSumPattern = getFixChkSumTagPattern();
    private final Bytes value = Bytes.elasticByteBuffer();
    private FixInStreamCallback callback;

    private long startReadPosition;
    private long stopReadPosition;
    private int headerLen = 0;
    private int offset = 0;
    private int tagNum;
    private boolean isBody = false;

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
                    final long endOfMsgIdx = chkSumIdx + CHKSUM_FILES_LEN;
                    onReadFixMessage(endOfMsgIdx);
                    buffer.clear();
                } else {
                    //buffer.compact();
                    break;
                }
            }
        }
    }

    private void onReadFixMessage(final long nextMsgOffset) {
        this.headerLen = 0;
        this.startReadPosition = buffer.readPosition();
        this.stopReadPosition = startReadPosition + nextMsgOffset;
        doFieldIterator(nextMsgOffset);
        isBody = false;
        callback.onMessageEnd(headerLen);
    }

    private FixInStreamCallback.StreamBehavior handleField() {
        if (!this.isBody) {
            this.isBody = !FixMessageUtil.isHeaderField(this.tagNum);
            if (!isBody) {
                this.headerLen = (int) (buffer.readPosition() - this.startReadPosition);
            }
        }
        return callback.onField(isBody, tagNum, value);
    }

    private void doFieldIterator(final long nextMsgOffset) {
        callback.onMessageBegin(buffer, this.startReadPosition, nextMsgOffset);
        while (buffer.readPosition() < stopReadPosition) {
            value.clear();
            doTag();
            doValue();
            if (handleField() == FixInStreamCallback.StreamBehavior.BREAK) {
                buffer.readPosition(stopReadPosition);
                break;
            }
        }
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