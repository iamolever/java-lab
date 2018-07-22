package org.ovr.javalab.fixnio;

import com.lmax.nanofix.FixClient;
import com.lmax.nanofix.FixClientFactory;
import com.lmax.nanofix.fields.EncryptMethod;
import com.lmax.nanofix.fields.MsgType;
import com.lmax.nanofix.outgoing.FixMessageBuilder;
import com.lmax.nanofix.transport.ConnectionObserver;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ovr.javalab.fixnio.connection.FixConnectionContext;
import org.ovr.javalab.fixnio.core.FixEngine;
import org.ovr.javalab.fixnio.core.FixMessageInHandler;
import org.ovr.javalab.fixnio.core.FixMessageInEvent;
import org.ovr.javalab.fixnio.session.FixSessionState;
import org.ovr.javalab.fixnio.session.FixSessionStateConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class FixServerTest {
    private final static Logger logger = LoggerFactory.getLogger(FixServerTest.class);

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
                .encryptMethod(EncryptMethod.NONE)
                .heartBtInt(30)
                .username("user1")
                .build();
    }

    @Test
    void smokeTest() throws InterruptedException {
        final CountDownLatch msgLeftToReceive = new CountDownLatch(1);
        final FixMessageInHandler inHandler = fixServer.newEventHandler();

        final Consumer<FixConnectionContext> connectionHandler = (context) -> {
            inHandler.handleEventsFrom(context);
            logger.debug("New connection on server. Context: {}", context);
        };
        final Consumer<FixConnectionContext> readHandler = (context) -> {
            logger.debug("Read event: {}", context.getReadBuffer().toDebugString());
        };
        final Consumer<FixConnectionContext> writeHandler = (context) -> {
            //logger.debug("Write event: {}", context);
        };
        final Consumer<FixMessageInEvent> newSessionHandler = (event) -> {
            logger.debug("Accepted FIX session: {}", event.getFixConnectionContext().getFixSession());
        };
        final FixSessionStateConsumer sessionStateHandler = (session, prevState, newState) -> {
            logger.debug("FIX Session '{}-{}' state changed from {} to {}",
                    session.getSenderCompId(), session.getTargetCompId(), prevState, newState);
        };
        final Consumer<FixMessageInEvent> messageHandler = (event) -> {
            logger.debug("FIX incoming event: {}", event);
            msgLeftToReceive.countDown();
        };
        fixServer.handleSocketConnectionWith(connectionHandler);
        fixServer.handleSocketReadEventWith(readHandler);
        fixServer.handleSocketWriteEventWith(writeHandler);
        fixServer.handleNewFixSessionWith(newSessionHandler);
        fixServer.handleFixSessionStateWith(sessionStateHandler);
        fixServer.start();

        inHandler.handleEventsWith(messageHandler);
        inHandler.run();

        final FixClient client = FixClientFactory.createFixClient(host, port);
        client.subscribeToAllMessages(fixMessage -> System.out.println("Received fix message " + fixMessage.toFixString()));

        client.registerTransportObserver(new ConnectionObserver() {
            @Override
            public void connectionEstablished() {
                logger.debug("TCP Connection to server has been established");
            }

            @Override
            public void connectionClosed() {
                System.out.println("TCP Connection to server has been closed");
            }
        });
        client.connect();

        assertTrue(client.isConnected());

        client.send(buildLogon());
        msgLeftToReceive.await(1000, TimeUnit.MILLISECONDS);
        assertEquals(0, msgLeftToReceive.getCount());
    }
}
