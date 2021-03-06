package org.ovr.javalab.fixnio.core;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Maths;
import org.ovr.javalab.fixnio.connection.FixConnectionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class SocketEventLoop implements Closeable, Runnable {
    private final Logger logger = LoggerFactory.getLogger(SocketEventLoop.class);

    private final static boolean DEFAULT_TCP_NODELAY = true;

    private final String networkInterfaceName;
    private final int port;
    private final Selector selector;
    private final ServerSocketChannel serverSocket;

    private Consumer<FixConnectionContext> socketConnectionHandler;
    private Consumer<FixConnectionContext> socketReadHandler;
    private Consumer<FixConnectionContext> socketWriteHandler;

    private volatile boolean stopFlag = false;
    private boolean tcpNoDelay = DEFAULT_TCP_NODELAY;

    public SocketEventLoop(final String networkInterfaceName, final int port) throws IOException {
        this.networkInterfaceName = networkInterfaceName;
        this.port = port;
        selector = Selector.open();
        serverSocket = ServerSocketChannel.open();
        init();
    }

    private void init() throws IOException {
        InetSocketAddress socketAddress;
        if (Optional.ofNullable(this.networkInterfaceName).isPresent()) {
            final NetworkInterface nif = NetworkInterface.getByName(this.networkInterfaceName);
            Enumeration<InetAddress> nifAddresses = nif.getInetAddresses();
            socketAddress = new InetSocketAddress(nifAddresses.nextElement(), port);
        } else {
            socketAddress = new InetSocketAddress(port);
        }
        serverSocket.bind(socketAddress);
        serverSocket.configureBlocking(false);
        serverSocket.register(selector, SelectionKey.OP_ACCEPT);
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

    public void doEventLoop() throws IOException {
        stopFlag = false;

        while (!stopFlag) {
            selector.select();
            final Set<SelectionKey> selectedKeys = selector.selectedKeys();
            final Iterator<SelectionKey> eventIterator = selectedKeys.iterator();
            while (eventIterator.hasNext()) {
                final SelectionKey key = eventIterator.next();
                eventIterator.remove();

                if (!key.isValid()) { //is it really required?
                    continue;
                }

                if (key.isReadable()) {
                    handleReadableEvent(key);
                } else if (key.isWritable()) {
                    handleWritableEvent(key);
                } else if (key.isAcceptable()) {
                    handleAcceptableEvent(key);
                }
            }
        }
    }

    private void clearOrCompactInBuffer(final Bytes buffer, final ByteBuffer inBB) {
        if (buffer.readRemaining() == 0) {
            inBB.clear();
        } else if (buffer.readPosition() > 0) {
            // if it read some data compact();
            inBB.position((int) buffer.readPosition());
            inBB.limit((int) buffer.readLimit());
            inBB.compact();
            buffer.readPosition(0);
            buffer.readLimit(inBB.remaining());
        }
    }

    private void handleReadableEvent(final SelectionKey key) throws IOException {
        final FixConnectionContext context = (FixConnectionContext) key.attachment();
        final ByteBuffer inBB = context.getInByteBuffer();
        final SocketChannel client = (SocketChannel) key.channel();
        final int read = client.read(inBB);
        if (read > 0) {
            final Bytes buffer = context.getReadBuffer();
            buffer.readLimit(inBB.position());
            if (socketReadHandler != null) {
                this.socketReadHandler.accept(context);
            }
            context.getInStreamHandler().onRead();
            clearOrCompactInBuffer(buffer, inBB);
        }
    }

    private void handleWritableEvent(final SelectionKey key) throws IOException {
        final FixConnectionContext context = (FixConnectionContext) key.attachment();
        if (socketWriteHandler != null) {
            this.socketWriteHandler.accept(context);
        }
        final Bytes writeBuffer = context.getWriteBuffer();
        final ByteBuffer outBB = context.getOutByteBuffer();
        final SocketChannel client = (SocketChannel) key.channel();
        context.prepareOutgoingBuffer();
        if (context.hasDataToWrite(outBB)) {
            outBB.limit(Maths.toInt32(writeBuffer.writePosition()));
            final int wrote = client.write(outBB);
            if (wrote > 0) {
                outBB.compact().flip();
                context.getWriteBuffer().writePosition(outBB.limit());
            }
        }
    }

    private void handleAcceptableEvent(final SelectionKey key) throws IOException {
        final SocketChannel client = serverSocket.accept();
        client.socket().setTcpNoDelay(tcpNoDelay);
        client.configureBlocking(false);
        final SelectionKey clientKey = client.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        final FixConnectionContext context = new FixConnectionContext(client);
        clientKey.attach(context);
        this.socketConnectionHandler.accept(context);
    }

    public void shutdown() {
        this.stopFlag = true;
        this.selector.wakeup();
    }

    @Override
    public void run() {
        try {
            doEventLoop();
        } catch (IOException e) {
            logger.error("Error in socket event loop", e);
        }
    }

    @Override
    public void close() throws IOException {
        this.selector.close();
        this.serverSocket.close();
    }
}
