package org.ovr.javalab.fixnio.core;

@FunctionalInterface
interface FixEventInHandler {
    void onFixEvent(FixMessageInEvent inEvent);
}
