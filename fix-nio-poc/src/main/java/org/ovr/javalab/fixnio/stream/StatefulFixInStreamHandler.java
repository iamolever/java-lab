package org.ovr.javalab.fixnio.stream;

import net.openhft.chronicle.bytes.Bytes;
import org.ovr.javalab.fixmsg.FixMessageHeader;
import org.ovr.javalab.fixmsg.util.ByteUtil;
import org.ovr.javalab.fixmsg.util.FixMessageUtil;

import static org.ovr.javalab.fixnio.stream.FixInStreamCallback.StreamBehavior.BREAK;

@Deprecated
public class StatefulFixInStreamHandler implements FixInputStreamHandler {
    private final static int DEFAULT_COMPACT_THRESHOLD = 32678;
    //private final Logger logger = LoggerFactory.getLogger(StatefulFixInStream.class);

    private final Bytes buffer;
    private int compactThreshold = DEFAULT_COMPACT_THRESHOLD;
    private final Bytes value = Bytes.elasticByteBuffer();
    private FixInStreamCallback callback;

    private String errorDesc;
    private int tagNum;

    private long offset = 0;
    private StreamState streamState = StreamState.INIT;
    private FixInStreamCallback.StreamBehavior streamBehavior = FixInStreamCallback.StreamBehavior.CONTINUE;

    public StatefulFixInStreamHandler(final Bytes buffer, final FixInStreamCallback callback, final int compactThreshold ) {
        this.buffer = buffer;
        this.callback = callback;
        this.compactThreshold = compactThreshold;
        this.onInitState();
    }

    public StatefulFixInStreamHandler(final Bytes buffer, final FixInStreamCallback callback) {
        this.buffer = buffer;
        this.callback = callback;
        this.onInitState();
    }

    @Override
    public Bytes getBuffer() {
        return buffer;
    }

    @Override
    public void onRead() {
        /*logger.trace("On event: read {} bytes in {} | Stream state: {}@{}",
                buffer.readRemaining(), buffer, streamState, offset);*/

        while (buffer.readRemaining() > 0) {
            onRead(buffer.readByte(buffer.readPosition() + offset));
        }
    }

    private void onRead(final byte readByte) {
        //logger.trace("[state={}, rpos={}, rream={}, offset={}, ch='{}']", streamState, buffer.readPosition(), buffer.readRemaining(), offset, (char) readByte);
        switch (streamState) {
            case VALUE_PROCESSING:
                handleInValueProcessingState(readByte);
                break;
            case TAG_PROCESSING:
                handleInTagProcessingState(readByte);
                break;
            case WAIT_FOR_FIX_BEGIN:
                handleInWaitForMessageState(readByte);
                break;
            case INIT:
                handleInInitState(readByte);
                break;
            case ERROR:
                break;
        }
    }

    private boolean isAsciiEquals(final byte readByte) {
        return FixMessageUtil.isAsciiEquals(readByte);
    }

    private boolean isAsciiSOH(final byte readByte) {
        return FixMessageUtil.isAsciiSOH(readByte);
    }

    private void handleInInitState(final byte readByte) {
        offset++;
        switchState(StreamState.WAIT_FOR_FIX_BEGIN);
    }

    private void handleInTagProcessingState(final byte readByte) {
        if (isAsciiEquals(readByte)) {
            readTag();
            doTransitionToValueProcessing();
        } else {
            offset++;
        }
    }

    private void handleEndOfMessage() {
        callback.onMessageEnd();
        switchState(StreamState.WAIT_FOR_FIX_BEGIN);
        onWaitForMessage();
        if (buffer.writePosition() > compactThreshold) {
            buffer.compact();
            //logger.trace("buffer compact: {}", buffer.toDebugString());
        }
    }

    private void handleTagValue() {
        readValue();
        streamBehavior = callback.onField(tagNum, value);
    }

    private void handleInValueProcessingState(final byte readByte) {
        if (isAsciiSOH(readByte)) {
            handleTagValue();
            if (tagNum == FixMessageHeader.CheckSum || streamBehavior == BREAK) {
                handleEndOfMessage();
            } else {
                doTransitionToTagProcessing();
            }
        } else {
            offset++;
        }
    }

    private void handleMessageBegin() {
        callback.onMessageBegin(buffer, 0, 0);
        streamBehavior = FixInStreamCallback.StreamBehavior.CONTINUE;
        doTransitionToValueProcessing();
    }

    private void handleInWaitForMessageState(final byte readByte) {
        if (isAsciiEquals(readByte)) {
            readTag();
            if (tagNum == FixMessageHeader.BeginString) {
                handleMessageBegin();
            } else {
                skipOrDoTransitionToError("BeginString(8) is expected in current state.");
            }
        } else if (isAsciiSOH(readByte)) {
            skipOrDoTransitionToError("SOH is not expected in current state.");
        } else {
            offset++;
        }
    }

    private void doTransitionToValueProcessing() {
        switchState(StreamState.VALUE_PROCESSING);
        onValueProcessing();
    }

    private void doTransitionToTagProcessing() {
        switchState(StreamState.TAG_PROCESSING);
        onTagProcessing();
    }

    private void readTag() {
        this.tagNum = ByteUtil.readIntFromBuffer(buffer, 0, offset);
    }

    private void readValue() {
        buffer.read(value, (int) offset);
        offset = 0;
    }

    private void savePosition() {
        this.buffer.readSkip(offset + 1);
        offset = 0;
    }

    private void onInitState() {
    }

    private void onWaitForMessage() {
        savePosition();
    }

    private void onTagProcessing() {
        savePosition();
    }

    private void onValueProcessing() {
        value.clear();
        savePosition();
    }

    private void skipOrDoTransitionToError(final String error) {
        if (streamBehavior == BREAK) {
            savePosition();
        } else {
            doTransitionToError(error);
        }
    }

    private void doTransitionToError(final String error) {
        this.errorDesc = error;
        switchState(StreamState.ERROR);
        onErrorState();
    }

    private void onErrorState() {
        throw new IllegalStateException(errorDesc);
    }

    private void switchState(final StreamState newStreamState) {
        //logger.trace("Switched state from {} to {}", this.streamState, newStreamState);
        this.streamState = newStreamState;
    }

    public enum StreamState {
        INIT,
        WAIT_FOR_FIX_BEGIN,
        TAG_PROCESSING,
        VALUE_PROCESSING,
        ERROR
    }
}
