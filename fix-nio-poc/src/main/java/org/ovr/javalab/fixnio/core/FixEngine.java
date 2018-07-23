package org.ovr.javalab.fixnio.core;

import net.openhft.chronicle.bytes.Bytes;
import org.ovr.javalab.fixmsg.FixField;
import org.ovr.javalab.fixmsg.FixMessage;
import org.ovr.javalab.fixmsg.MessageType;
import org.ovr.javalab.fixmsg.util.FixMessageEncoder;
import org.ovr.javalab.fixnio.common.FixMessageCircularQueue;
import org.ovr.javalab.fixnio.connection.FixConnectionContext;
import org.ovr.javalab.fixnio.session.FixSessionRegistry;
import org.ovr.javalab.fixnio.session.FixSessionState;
import org.ovr.javalab.fixnio.session.FixSessionStateConsumer;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class FixEngine {
    private final static int DEF_OUT_QUEUE_CAPACITY = 1000;

    private final String host;
    private final int port;
    private FixSessionRegistry sessionRegistry = FixSessionRegistry.defaultInstance();

    private SocketEventLoop socketEventLoop;
    private Thread ioThread;

    private Consumer<FixConnectionContext> socketConnectionHandler;
    private Consumer<FixConnectionContext> socketReadHandler;
    private Consumer<FixConnectionContext> socketWriteHandler;

    private Consumer<FixMessageInEvent> newFixSessionHandler;
    private FixSessionStateConsumer fixSessionStateHandler;

    public FixEngine(final String host, final int port) throws IOException {
        this.host = host;
        this.port = port;
        initSocketLoopThread();
    }

    private void initSocketLoopThread() throws IOException {
        this.socketEventLoop = new SocketEventLoop(host, port);
        this.ioThread = Executors.defaultThreadFactory().newThread(socketEventLoop);
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

    public void handleNewFixSessionWith(final Consumer<FixMessageInEvent> newFixSessionHandler) {
        this.newFixSessionHandler = newFixSessionHandler;
    }

    public void handleFixSessionStateWith(final FixSessionStateConsumer fixSessionStateHandler) {
        this.fixSessionStateHandler = fixSessionStateHandler;
    }

    public void start() {
        socketEventLoop.handleSocketConnectionWith(this::handleFixConnection);
        socketEventLoop.handleSocketReadEventWith(this.socketReadHandler);
        socketEventLoop.handleSocketWriteEventWith(this.socketWriteHandler);
        ioThread.start();
    }

    public void shutdown() throws IOException, InterruptedException {
        socketEventLoop.shutdown();
        ioThread.join();
        socketEventLoop.close();
    }

    private void handleFixConnection(final FixConnectionContext context) {
        context.getFixSession().handleFixSessionStateWith(this.fixSessionStateHandler);
        context.getFixSession().switchState(FixSessionState.CONNECTING);
        if (this.socketConnectionHandler != null) {
            this.socketConnectionHandler.accept(context);
        }
    }

    public FixMessageInHandler newEventHandler() {
        return new FixMessageInHandler(this::onFixEvent);
    }

    public void onFixEvent(final FixMessageInEvent inEvent) {
        if (inEvent.isAdminEvent()) {
            final String msgType = inEvent.getFixMessage().getMsgType();
            if (msgType.equals(MessageType.Heartbeat.getProtocolValue())) {

            } else if(msgType.equals(MessageType.Logon.getProtocolValue())) {
                handleLogon(inEvent);
            }
        }
    }

    private void handleHeartbeat(final FixMessageInEvent inEvent) {

    }

    private void handleLogon(final FixMessageInEvent inEvent) {
        inEvent.getFixConnectionContext().initOutgoingQueue(DEF_OUT_QUEUE_CAPACITY);
        this.sessionRegistry.handleLogon(inEvent);
        if (this.newFixSessionHandler != null) {
            newFixSessionHandler.accept(inEvent);
        }
        sendLogonResponse(inEvent);
    }

    private void sendLogonResponse(final FixMessageInEvent inEvent) {
        final FixMessageCircularQueue queue = inEvent.getFixConnectionContext().getOutgoingQueue();
        synchronized (queue) {
            final FixMessage fixMessage = queue.getToPublish();
            fixMessage.setMsgType(MessageType.Logon.getProtocolValue());
            final Bytes bytes = fixMessage.getRawMessage();
            bytes.write(FixField.EncryptMethod.asString());
            FixMessageEncoder.completeTag(bytes);
            bytes.write("0");
            FixMessageEncoder.completeField(bytes);
            bytes.write(FixField.HeartBtInt.asString());
            FixMessageEncoder.completeTag(bytes);
            bytes.write("30");
            //FixMessageEncoder.completeField(bytes);
            queue.publish();
        }
    }
}
