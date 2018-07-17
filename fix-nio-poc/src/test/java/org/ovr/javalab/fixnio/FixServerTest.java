package org.ovr.javalab.fixnio;

import com.lmax.nanofix.FixClient;
import com.lmax.nanofix.FixClientFactory;
import com.lmax.nanofix.fields.MsgType;
import com.lmax.nanofix.outgoing.FixMessageBuilder;
import com.lmax.nanofix.transport.ConnectionObserver;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ovr.javalab.fixnio.connection.FixConnectionContext;
import org.ovr.javalab.fixnio.core.FixEngine;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class FixServerTest {
    private final String host = "localhost";
    private final int port = 2000;
    private FixEngine fixServer;

    @BeforeEach
    void beforeTest() throws IOException {
        fixServer = new FixEngine(host, port);
    }

    @AfterEach
    void afterTest() throws IOException, InterruptedException {
        fixServer.shutdown();
    }

    private com.lmax.nanofix.outgoing.FixMessage buildLogon() {
        return new FixMessageBuilder()
                .messageType(MsgType.LOGIN)
                .senderCompID("sender1")
                .targetCompID("target1")
                .msgSeqNum(1)
                .sendingTime(DateTime.now())
                .username("user1")
                .build();
    }

    @Test
    void testAcceptorConnectAndLogon() throws InterruptedException {
        final CountDownLatch msgLeftToReceive = new CountDownLatch(1);

        final Consumer<FixConnectionContext> connectionHandler = (context) -> {
            System.out.println("New connection on server");
        };
        final Consumer<FixConnectionContext> readHandler = (context) -> {
            System.out.println("read event: " + context.getReadBuffer().toDebugString());
            msgLeftToReceive.countDown();
        };
        /*final Consumer<FixMessage> messageHandler = (context) -> {
            System.out.println(context.getReadBuffer().toDebugString());
            msgLeftToReceive.countDown();
        };*/
        fixServer.handleSocketConnectionWith(connectionHandler);
        fixServer.handleSocketReadEventWith(readHandler);
        fixServer.start();

        final FixClient client = FixClientFactory.createFixClient(host, port);
        client.subscribeToAllMessages(fixMessage -> System.out.println("Received fix message " + fixMessage.toFixString()));

        client.registerTransportObserver(new ConnectionObserver() {
            @Override
            public void connectionEstablished() {
                System.out.println("TCP Connection has been established.");
            }

            @Override
            public void connectionClosed() {
                System.out.println("TCP Connection Closed.");
            }
        });
        client.connect();

        assertTrue(client.isConnected());

        client.send(buildLogon());
        msgLeftToReceive.await(1000, TimeUnit.MILLISECONDS);
        assertEquals(0, msgLeftToReceive.getCount());
    }
}
