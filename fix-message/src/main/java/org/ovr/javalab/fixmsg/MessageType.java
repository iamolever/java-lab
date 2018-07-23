package org.ovr.javalab.fixmsg;

public enum MessageType {
    Heartbeat("0"),
    Logon("A");

    private final String protocolValue;

    MessageType(final String protocolValue) {
        this.protocolValue = protocolValue;
    }

    public String getProtocolValue() {
        return protocolValue;
    }
}
