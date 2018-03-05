package org.ovr.javalab.fixnio;

import com.lmax.nanofix.FixClient;
import com.lmax.nanofix.FixClientFactory;
import com.lmax.nanofix.fields.MsgType;
import com.lmax.nanofix.incoming.FixMessage;
import com.lmax.nanofix.incoming.FixMessageHandler;
import com.lmax.nanofix.outgoing.FixMessageBuilder;
import com.lmax.nanofix.transport.ConnectionObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class FixServiceTest {
    private final String host = "localhost";
    private final int port = 2000;
    private FixService fixService;
    private Thread fixServiceThread;

    @BeforeEach
    void beforeTest() {
    }

    @AfterEach
    void afterTest() throws IOException, InterruptedException {
        fixService.shutdown();
        fixServiceThread.join();
        fixService.close();
    }

    @Test
    void testAcceptorConnectAndLogon() throws InterruptedException {
        final CountDownLatch msgReceived = new CountDownLatch(1);

        final Consumer<FixConnectionContext> handler = (context) -> {
            System.out.println(context.readBuffer.toDebugString());
            msgReceived.countDown();
        };

        fixServiceThread = new Thread(() -> {
            try {
                fixService = new FixService(host, port);
                fixService.doEventLoop(handler);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        fixServiceThread.start();

        final FixClient client = FixClientFactory.createFixClient(host, port);
        client.subscribeToAllMessages(new FixMessageHandler() {
            @Override
            public void onFixMessage(FixMessage fixMessage) {
                System.out.println("Received fix message " + fixMessage.toFixString());
            }
        });

        client.registerTransportObserver(new ConnectionObserver() {
            @Override
            public void connectionEstablished() {
                System.out.println("TCP Connection Established.");
            }

            @Override
            public void connectionClosed() {
                System.out.println("TCP Connection Closed.");
            }
        });
        client.connect();

        assertTrue(client.isConnected());

        client.send(new FixMessageBuilder().messageType(MsgType.LOGIN).username("hello").build());
        msgReceived.await(100, TimeUnit.MILLISECONDS);
        assertEquals(0, msgReceived.getCount());
    }
}
