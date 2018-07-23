package org.ovr.javalab.fixnio.session;

import org.ovr.javalab.fixnio.connection.FixConnectionContext;
import org.ovr.javalab.fixnio.core.FixMessageInEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySessionRegistry implements FixSessionRegistry {
    private Map<String, FixConnectionContext> senderCompIdIndex = new ConcurrentHashMap<>();
    private Map<String, FixConnectionContext> targetCompIdIndex = new ConcurrentHashMap<>();

    @Override
    public void handleLogon(final FixMessageInEvent inEvent) {
        final FixSession fixSession = inEvent.getFixConnectionContext().getFixSession();
        fixSession.initFromLogon(inEvent.getFixMessage());
        senderCompIdIndex.put(fixSession.getSenderCompId(), inEvent.getFixConnectionContext());
        targetCompIdIndex.put(fixSession.getTargetCompId(), inEvent.getFixConnectionContext());
    }

    @Override
    public FixConnectionContext findBySenderCompId(final String senderCompId) {
        return this.senderCompIdIndex.get(senderCompId);
    }

    @Override
    public FixConnectionContext findByTargetCompId(final String targetCompId) {
        return this.targetCompIdIndex.get(targetCompId);
    }
}
