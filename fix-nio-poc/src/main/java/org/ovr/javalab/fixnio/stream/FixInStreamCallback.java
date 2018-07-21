package org.ovr.javalab.fixnio.stream;

import net.openhft.chronicle.bytes.Bytes;

public interface FixInStreamCallback {

    void onMessageBegin(final Bytes buffer, final long offset, final long length);

    void onMessageEnd(final int headerLen);

    StreamBehavior onField(final boolean isBodyField, final int tagNum, final Bytes buffer);

    AfterErrorBehavior onError(final String errorDesc);

    enum StreamBehavior {
        CONTINUE, BREAK
    }

    enum AfterErrorBehavior {
        CONTINUE, BREAK, FATAL
    }
}
