package org.ovr.javalab.fixnio;

import com.lmax.nanofix.FixClient;
import com.lmax.nanofix.FixClientFactory;
import com.lmax.nanofix.outgoing.FixMessageBuilder;
import com.lmax.nanofix.transport.ConnectionObserver;
import org.joda.time.DateTime;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.ovr.javalab.fixnio.FixServerTest.buildLogon;

public class RouterTest {
    private final static Logger logger = LoggerFactory.getLogger(RouterTest.class);

    private final String host = "localhost";
    private final int port = 2000;
    private final int generatorClientNum = 50;
    private final int sinkClientNum = 200;

    @Test
    @Ignore("Manual test")
    public void smokeTest() throws Exception {
        final CountDownLatch logonLeftToReceive = new CountDownLatch(sinkClientNum + generatorClientNum);
        final CountDownLatch msgLeftToReceive = new CountDownLatch(sinkClientNum);

        final List<FixClient> genClients = new ArrayList<>(generatorClientNum);
        for (int i = 1; i <= generatorClientNum; i++) {
            final String clientId = "GENERATOR" + i;
            final FixClient client = FixClientFactory.createFixClient(host, port);
            client.subscribeToAllMessages(
                    fixMessage -> {
                        logger.debug("{}: Received fix message {}", clientId, fixMessage.toFixString());
                        if (fixMessage.getFirstValue(35).equals("A")) {
                            logonLeftToReceive.countDown();
                        }
                    });

            client.registerTransportObserver(new ConnectionObserver() {
                @Override
                public void connectionEstablished() {
                    logger.debug("GENERATOR: TCP Connection to server has been established");
                }

                @Override
                public void connectionClosed() {
                    logger.debug("GENERATOR: TCP Connection to server has been closed");
                }
            });
            client.connect();

            assertTrue(client.isConnected());
            client.send(buildLogon(String.format("generator%03d", i), String.format("fanout%03d", i), "user"+i));
            genClients.add(client);
            Thread.sleep(20);
        }
        final List<FixClient> sinkClients = new ArrayList<>(sinkClientNum);
        for (int i = 1; i <= sinkClientNum; i++) {
            final String clientId = "SINK" + i;
            final FixClient client = FixClientFactory.createFixClient(host, port);
            client.subscribeToAllMessages(
                    fixMessage -> {
                        logger.debug("{}: Received fix message {}", clientId, fixMessage.toFixString());
                        if (fixMessage.getFirstValue(35).equals("A")) {
                            logonLeftToReceive.countDown();
                        } else {
                            msgLeftToReceive.countDown();
                        }
                    });

            client.registerTransportObserver(new ConnectionObserver() {
                @Override
                public void connectionEstablished() {
                    logger.debug("SINK: TCP Connection to server has been established");
                }

                @Override
                public void connectionClosed() {
                    logger.debug("SINK: TCP Connection to server has been closed");
                }
            });
            client.connect();

            assertTrue(client.isConnected());
            client.send(buildLogon(String.format("sink%03d", i), String.format("src%03d", i), "user"+i));
            sinkClients.add(client);
            Thread.sleep(20);
        }
        logonLeftToReceive.await(10, TimeUnit.SECONDS);
        assertEquals(0, logonLeftToReceive.getCount());

        for (int i = 1; i <= generatorClientNum; i++) {
            genClients.get(i - 1).send(
                    buildNews(
                            String.format("generator%03d", i),
                            String.format("fanout%03d", i),
                            String.format("forward it from generator #%03d!", i)));
        }

        msgLeftToReceive.await(10, TimeUnit.SECONDS);
        assertEquals(0, msgLeftToReceive.getCount());
    }

    static com.lmax.nanofix.outgoing.FixMessage buildNews(final String senderId, final String targetId, final String text) {
        return new FixMessageBuilder()
                .messageType("B")
                .senderCompID(senderId)
                .targetCompID(targetId)
                .msgSeqNum(2)
                .sendingTime(DateTime.now())
                .append(148, "important")
                .append(33, "1")
                .append(58, text)
                .build();
    }

}
