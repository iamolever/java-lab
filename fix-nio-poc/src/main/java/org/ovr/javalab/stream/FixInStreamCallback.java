package org.ovr.javalab.stream;

import net.openhft.chronicle.bytes.Bytes;

public interface FixInStreamCallback {

    void onMessageBegin();
    void onMessageEnd();
    AfterErrorBehavior onError(final String errorDesc);
    StreamBehavior onTagValue(final int tagNum, final Bytes buffer);

    enum StreamBehavior {
        CONTINUE, NEXT_MESSAGE
    }

    enum AfterErrorBehavior {
        CONTINUE, NEXT_MESSAGE, BREAK;
    }
}
