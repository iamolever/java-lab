package org.ovr.javalab.fixnio.router;

import org.ovr.javalab.fixnio.connection.FixConnectionContext;
import org.ovr.javalab.fixnio.core.FixEngine;
import org.ovr.javalab.fixnio.core.FixMessageInEvent;
import org.ovr.javalab.fixnio.core.FixMessageInHandler;
import org.ovr.javalab.fixnio.session.FixSessionStateConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class RouterServer {
    private final static Logger logger = LoggerFactory.getLogger(RouterServer.class);

    private String networkInterface;
    private int port;
    private int workerThreadNum;
    private final FixEngine fixEngine;
    private final WorkerPool pool;
    private final Router router;

    public RouterServer(String networkInterface, int port, int workerThreadNum) throws IOException {
        this.networkInterface = networkInterface;
        this.port = port;
        this.workerThreadNum = workerThreadNum;
        this.fixEngine = new FixEngine(networkInterface, port);
        this.pool = new WorkerPool(this.fixEngine, this.workerThreadNum);
        this.router = new Router();
    }

    private void subscribe() {
        final Consumer<FixMessageInEvent> messageHandler = (event) -> {
            router.routeMessage(event);
        };

        final Consumer<FixConnectionContext> connectionHandler = (context) -> {
            logger.info("New connection on server. Context: {}", context);
            pool.handleEventsFrom(context, messageHandler);
        };

        final Consumer<FixMessageInEvent> newSessionHandler = (event) -> {
            router.handleNewConnection(event.getFixConnectionContext());
            logger.info("Accepted FIX session: {}", event.getFixConnectionContext().getFixSession());
        };

        final FixSessionStateConsumer sessionStateHandler = (session, prevState, newState) -> {
            logger.info("FIX Session '{}-{}' state changed from {} to {}",
                    session.getSenderCompId(), session.getTargetCompId(), prevState, newState);
        };

        fixEngine.handleSocketConnectionWith(connectionHandler);
        fixEngine.handleNewFixSessionWith(newSessionHandler);
        fixEngine.handleFixSessionStateWith(sessionStateHandler);
    }

    public void start() {
        pool.start();
        fixEngine.start();
    }

    private class Router {
        private final int SESSIONS_IN_GROUP = 4;
        private final Map<String, List<FixConnectionContext>> routerRules = new ConcurrentHashMap<>();

        public void handleNewConnection(final FixConnectionContext context) {
            final String target = context.getFixSession().getTargetCompId();
            final int num = Integer.parseInt(target.substring(target.length() - 3));
            if (target.startsWith("generator")) {
                updateRouterRule(num);
            } else {
                updateRouterRule(num%SESSIONS_IN_GROUP == 0 ? num/SESSIONS_IN_GROUP : num/SESSIONS_IN_GROUP + 1);
            }
        }

        private void updateRouterRule(final int num) {
            final String generatorCompId = String.format("generator%03d", num);
            final List<FixConnectionContext> destinations = new ArrayList<>();

            for (int i = 1; i <= SESSIONS_IN_GROUP; i++) {
                final int dstNum = (num-1) * SESSIONS_IN_GROUP + i;
                final String targetCompID = String.format("sink%03d", dstNum);
                final FixConnectionContext context = fixEngine.getSessionRegistry().findByTargetCompId(targetCompID);
                if (context != null) {
                    destinations.add(context);
                }
            }
            this.routerRules.put(generatorCompId, destinations);
        }

        public void routeMessage(final FixMessageInEvent event) {
            final String generatorCompId = event.getFixMessage().getSenderCompId();
            final List<FixConnectionContext> destinations = routerRules.get(generatorCompId);
            if (destinations != null) {
                for (int i = 0; i < destinations.size(); i++) {
                     destinations.get(i).getFixSession().send(event.getFixMessage());
                }
            }
        }
    }

    private class WorkerPool {
        private final int size;
        private final FixMessageInHandler[] eventInHandler;
        private int sequence = 0;

        public WorkerPool(final FixEngine fixEngine, final int size) {
            this.size = size;
            eventInHandler = new FixMessageInHandler[size];
            createWorkers(fixEngine);
        }

        private void createWorkers(final FixEngine fixEngine) {
            for (int i = 0; i < eventInHandler.length; i++) {
                eventInHandler[i] = fixEngine.newEventHandler();
            }
        }

        public void handleEventsFrom(final FixConnectionContext context, Consumer<FixMessageInEvent> messageHandler) {
            final int workerNum = sequence % size;
            eventInHandler[workerNum].handleEventsFrom(context);
            eventInHandler[workerNum].handleEventsWith(messageHandler);
            logger.info("Worker #{} will handle connection {}", workerNum, context);
            sequence++;
        }

        public void start() {
            for (int i = 0; i < eventInHandler.length; i++) {
                Executors.defaultThreadFactory().newThread(eventInHandler[i]).start();
            }
        }
    }

    public static void main(final String[] args) {
        try {
            final String networkInterface = args[0];
            final int port = Integer.parseInt(args[1]);
            final int workerThreadNum = Integer.parseInt(args[2]);
            final RouterServer routerServer = new RouterServer(
                    networkInterface.equals("lo") ? null : networkInterface,
                    port,
                    workerThreadNum);
            routerServer.subscribe();
            routerServer.start();
            logger.info("Router server is started");
        } catch (IOException e) {
            logger.error("Failed to start Router", e);
        }
    }
}
