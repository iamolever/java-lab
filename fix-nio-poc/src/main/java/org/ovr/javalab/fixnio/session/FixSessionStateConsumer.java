package org.ovr.javalab.fixnio.session;

@FunctionalInterface
public interface FixSessionStateConsumer {
    void onStateChange(final FixSession fixSession, final FixSessionState prevState, final FixSessionState newState);
}
