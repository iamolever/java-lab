package org.ovr.javalab.fixnio.stream;

import net.openhft.chronicle.bytes.Bytes;

public interface FixInStreamCallback {

    void onMessageBegin(final Bytes buffer, final long offset, final long length);

    void onMessageEnd();

    StreamBehavior onField(final int tagNum, final Bytes buffer);

    AfterErrorBehavior onError(final String errorDesc);

    enum StreamBehavior {
        CONTINUE, BREAK
    }

    enum AfterErrorBehavior {
        CONTINUE, BREAK, FATAL
    }
}
