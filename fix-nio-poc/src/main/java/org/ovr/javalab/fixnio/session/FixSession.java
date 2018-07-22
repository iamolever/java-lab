package org.ovr.javalab.fixnio.session;

import org.ovr.javalab.fixmsg.FixMessage;

import java.util.concurrent.atomic.AtomicLong;

public interface FixSession {
    void initFromLogon(final FixMessage logonMessage);

    String getSenderCompId();
    String getTargetCompId();

    FixSessionState getState();
    void setState(final FixSessionState state);
    void switchState(final FixSessionState newState);

    AtomicLong getInSequence();
    //void setInSequence(final AtomicLong inSequence);

    AtomicLong getOutSequence();
    //void setOutSequence(final AtomicLong outSequence);

    void handleFixSessionStateWith(final FixSessionStateConsumer fixSessionStateHandler);

    void send(final FixMessage fixMessage);
}
