package org.ovr.javalab.fixnio.session;

import org.ovr.javalab.fixnio.core.FixMessageInEvent;

public interface FixSessionRegistry {
    static FixSessionRegistry defaultInstance() {
        return new InMemorySessionRegistry();
    }

    void handleLogon(final FixMessageInEvent logonMessage);
}
