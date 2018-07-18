package org.ovr.javalab.fixnio.core;

import org.ovr.javalab.fixnio.connection.FixConnectionContext;
import org.ovr.javalab.fixnio.session.FixSession;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class FixEngine {
    private final String host;
    private final int port;
    private SocketEventLoop socketEventLoop;
    private Thread fixServiceThread;
    private Consumer<FixConnectionContext> socketConnectionHandler;
    private Consumer<FixConnectionContext> socketReadHandler;
    private Consumer<FixConnectionContext> socketWriteHandler;

    private Consumer<FixConnectionContext> newFixSessionHandler;
    private Consumer<FixSession> fixSessionStateHandler;
    private Consumer<FixConnectionContext> fixMessageHandler;

    public FixEngine(final String host, final int port) throws IOException {
        this.host = host;
        this.port = port;
        initSocketLoopThread();
    }

    private void initSocketLoopThread() throws IOException {
        this.socketEventLoop = new SocketEventLoop(host, port);
        this.fixServiceThread = Executors.defaultThreadFactory().newThread(socketEventLoop);
    }

    public void handleSocketConnectionWith(final Consumer<FixConnectionContext> socketConnectionHandler) {
        this.socketConnectionHandler = socketConnectionHandler;
    }

    public void handleSocketReadEventWith(final Consumer<FixConnectionContext> socketReadHandler) {
        this.socketReadHandler = socketReadHandler;
    }

    public void handleSocketWriteEventWith(final Consumer<FixConnectionContext> socketWriteHandler) {
        this.socketWriteHandler = socketWriteHandler;
    }

    public void handleNewFixSessionWith(final Consumer<FixConnectionContext> newFixSessionHandler) {
        this.newFixSessionHandler = newFixSessionHandler;
    }

    public void handleFixSessionStateWith(final Consumer<FixSession> fixSessionStateHandler) {
        this.fixSessionStateHandler = fixSessionStateHandler;
    }

    public void handleFixMessageWith(final Consumer<FixConnectionContext> fixMessageHandler) {
        this.fixMessageHandler = fixMessageHandler;
    }

    public void start() {
        socketEventLoop.handleSocketConnectionWith(this::handleFixConnection);
        socketEventLoop.handleSocketReadEventWith(this.socketReadHandler);
        socketEventLoop.handleSocketWriteEventWith(this.socketWriteHandler);
        fixServiceThread.start();
    }

    public void shutdown() throws IOException, InterruptedException {
        socketEventLoop.shutdown();
        fixServiceThread.join();
        socketEventLoop.close();
    }

    private void handleFixConnection(final FixConnectionContext context) {
        if (this.socketConnectionHandler != null) {
            this.socketConnectionHandler.accept(context);
        }
    }
}
