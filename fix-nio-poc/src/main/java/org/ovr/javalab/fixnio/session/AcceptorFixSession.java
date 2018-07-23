package org.ovr.javalab.fixnio.session;

import net.openhft.chronicle.bytes.Bytes;
import org.ovr.javalab.fixmsg.FixMessage;
import org.ovr.javalab.fixmsg.util.FixMessageEncoder;
import org.ovr.javalab.fixnio.common.FixMessageCircularQueue;
import org.ovr.javalab.fixnio.connection.FixConnectionContext;

import java.util.concurrent.atomic.AtomicLong;

import static org.ovr.javalab.fixmsg.util.FixMessageUtil.CHKSUM_FILES_LEN;
import static org.ovr.javalab.fixnio.session.FixSessionState.DISCONNECTED;

public class AcceptorFixSession implements FixSession {
    private String senderCompId;
    private String targetCompId;
    private FixSessionState sessionState = DISCONNECTED;
    private FixSessionStateConsumer fixSessionStateHandler;
    //private final FixMessageCircularQueue outgoingQueue;
    private final AtomicLong inSequence = new AtomicLong();
    private final AtomicLong outSequence = new AtomicLong();
    private final FixConnectionContext connectionContext;

    public AcceptorFixSession(final FixConnectionContext context) {
        this.connectionContext = context;
    }

    public void initFromLogon(final FixMessage logonMessage) {
        this.senderCompId = logonMessage.getTargetCompId();
        this.targetCompId = logonMessage.getSenderCompId();
        switchState(FixSessionState.CONNECTED);
    }

    @Override
    public String getSenderCompId() {
        return this.senderCompId;
    }

    @Override
    public String getTargetCompId() {
        return this.targetCompId;
    }

    @Override
    public FixSessionState getState() {
        return this.sessionState;
    }

    @Override
    public void setState(final FixSessionState state) {
        this.sessionState = state;
    }

    @Override
    public void switchState(final FixSessionState newState) {
        final FixSessionState prevSessionState = this.sessionState;
        this.sessionState = newState;
        if (this.fixSessionStateHandler != null) {
            this.fixSessionStateHandler.onStateChange(this, prevSessionState, newState);
        }
    }

    @Override
    public AtomicLong getInSequence() {
        return this.inSequence;
    }

    @Override
    public AtomicLong getOutSequence() {
        return this.outSequence;
    }

    @Override
    public void handleFixSessionStateWith(final FixSessionStateConsumer fixSessionStateHandler) {
        this.fixSessionStateHandler = fixSessionStateHandler;
    }

    @Override
    public void send(final FixMessage fixMessage) {
        final FixMessageCircularQueue outgoingQueue = connectionContext.getOutgoingQueue();
        synchronized (outgoingQueue) {
            final FixMessage fixMessageInQueue = outgoingQueue.getToPublish();
            fixMessageInQueue.setMsgType(fixMessage.getMsgType());
            final Bytes bytes = fixMessageInQueue.getRawMessage();
            final long bodyLen = fixMessage.getRawMessage().readLimit() - fixMessage.getHeaderLength() - CHKSUM_FILES_LEN;
            bytes.write(fixMessage.getRawMessage(), fixMessage.getHeaderLength(), bodyLen);
            FixMessageEncoder.completeField(bytes);
            outgoingQueue.publish();
        }
    }

    @Override
    public String toString() {
        return "AcceptorFixSession{" +
                "senderCompId='" + senderCompId + '\'' +
                ", targetCompId='" + targetCompId + '\'' +
                '}';
    }
}
