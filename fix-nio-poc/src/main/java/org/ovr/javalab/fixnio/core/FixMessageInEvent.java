package org.ovr.javalab.fixnio.core;

import org.ovr.javalab.fixmsg.FixMessage;
import org.ovr.javalab.fixmsg.util.FixMessageUtil;
import org.ovr.javalab.fixnio.connection.FixConnectionContext;

public class FixMessageInEvent {
    private FixMessage fixMessage = FixMessage.instance();
    private FixConnectionContext fixConnectionContext;

    public FixMessage getFixMessage() {
        return fixMessage;
    }

    public FixConnectionContext getFixConnectionContext() {
        return fixConnectionContext;
    }

    public void setFixConnectionContext(final FixConnectionContext fixConnectionContext) {
        this.fixConnectionContext = fixConnectionContext;
    }

    public void clear() {
        this.fixConnectionContext = null;
        this.fixMessage.clear();
    }

    public boolean isAdminEvent() {
        return FixMessageUtil.isAdminMsg(this.fixMessage.getMsgType());
    }

    @Override
    public String toString() {
        return "FixMessageInEvent{" +
                "fixMessage=" + fixMessage +
                ", fixConnectionContext=" + fixConnectionContext +
                '}';
    }
}
