package org.ovr.javalab.fixnio;

import net.openhft.chronicle.bytes.Bytes;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;

public class FixService implements Closeable {
    private final String host;
    private final int port;
    private final Selector selector;
    private final ServerSocketChannel serverSocket;
    private volatile boolean stopFlag = false;

    public FixService(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        selector = Selector.open();
        serverSocket = ServerSocketChannel.open();
        init();
    }

    private void init() throws IOException {
        serverSocket.bind(new InetSocketAddress(host, port));
        serverSocket.configureBlocking(false);
        serverSocket.register(selector, SelectionKey.OP_ACCEPT);
    }

    public void doEventLoop(final Consumer<FixConnectionContext> consumer) throws IOException {
        stopFlag = false;

        while (!stopFlag) {
            selector.select();
            final Set<SelectionKey> selectedKeys = selector.selectedKeys();
            final Iterator<SelectionKey> eventIterator = selectedKeys.iterator();
            while (eventIterator.hasNext()) {
                final SelectionKey key = eventIterator.next();

                if (key.isReadable()) {
                    handleReadableEvent(key, consumer);
                } else if (key.isAcceptable()) {
                    handleAcceptableEvent(key);
                }
                eventIterator.remove();
            }
        }
    }

    private static void handleReadableEvent(final SelectionKey key, final Consumer<FixConnectionContext> consumer)
            throws IOException {
        final FixConnectionContext context = (FixConnectionContext) key.attachment();
        final Bytes buffer = context.readBuffer;
        final SocketChannel client = (SocketChannel) key.channel();
        client.read((ByteBuffer) buffer.underlyingObject());
        consumer.accept(context);
    }

    private void handleAcceptableEvent(final SelectionKey key) throws IOException {
        final SocketChannel client = serverSocket.accept();
        client.configureBlocking(false);
        final SelectionKey clientKey = client.register(selector, SelectionKey.OP_READ);
        final FixConnectionContext context = new FixConnectionContext(client);
        clientKey.attach(context);
    }

    public void shutdown() {
        this.stopFlag = true;
        this.selector.wakeup();
    }

    @Override
    public void close() throws IOException {
        this.selector.close();
        this.serverSocket.close();
    }
}
