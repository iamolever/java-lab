package org.ovr.javalab.fixnio.core;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.ovr.javalab.fixnio.connection.FixConnectionContext;
import org.ovr.javalab.fixnio.event.FixMessageInEventProducer;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class FixMessageInHandler implements Runnable, Closeable {
    private final static int DEFAULT_EVENT_BUFFER_SIZE = 1024;
    private int eventBufferSize;

    private final FixEventInHandler fixEventInHandler;
    private Disruptor<FixMessageInEvent> disruptor;
    private RingBuffer<FixMessageInEvent> ringBuffer;
    private Consumer<FixMessageInEvent> messageInEventConsumer;

    FixMessageInHandler(final FixEventInHandler fixEventInHandler) {
        this.fixEventInHandler = fixEventInHandler;
        this.eventBufferSize = DEFAULT_EVENT_BUFFER_SIZE;
        init();
    }

    FixMessageInHandler(final FixEventInHandler fixEventInHandler, final int eventBufferSize) {
        this.fixEventInHandler = fixEventInHandler;
        this.eventBufferSize = eventBufferSize;
        init();
    }

    private void init() {
        this.disruptor = new Disruptor<>(
                FixMessageInEvent::new, this.eventBufferSize, Executors.defaultThreadFactory(),
                ProducerType.SINGLE, new BlockingWaitStrategy());
        this.ringBuffer = disruptor.getRingBuffer();
        this.disruptor.handleEventsWith(this::handle);
    }

    public void handleEventsWith(final Consumer<FixMessageInEvent> messageInEventConsumer) {
        this.messageInEventConsumer = messageInEventConsumer;
    }

    public void handleEventsFrom(final FixConnectionContext context) {
        context.handleInStreamWith(new FixMessageInEventProducer(context, this.ringBuffer));
    }

    private void handle(final FixMessageInEvent event, final long sequence, final boolean endOfBatch) {
        this.fixEventInHandler.onFixEvent(event);
        if (!event.isAdminEvent()) {
            messageInEventConsumer.accept(event);
        }
        event.clear();
    }

    @Override
    public void run() {
        disruptor.start();
    }

    @Override
    public void close() throws IOException {
        disruptor.shutdown();
    }
}
