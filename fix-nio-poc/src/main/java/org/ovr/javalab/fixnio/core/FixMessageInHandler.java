package org.ovr.javalab.fixnio.core;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.ovr.javalab.fixnio.connection.FixConnectionContext;
import org.ovr.javalab.fixnio.event.FixMessageInEvent;
import org.ovr.javalab.fixnio.event.FixMessageInEventProducer;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class FixMessageInHandler implements Runnable, Closeable {
    private final static int DEFAULT_EVENT_BUFFER_SIZE = 1024;
    private int eventBufferSize;
    private Disruptor<FixMessageInEvent> disruptor;
    private RingBuffer<FixMessageInEvent> ringBuffer;
    private Consumer<FixMessageInEvent> messageInEventConsumer;

    public FixMessageInHandler() {
        this.eventBufferSize = DEFAULT_EVENT_BUFFER_SIZE;
        init();
    }

    public FixMessageInHandler(final int eventBufferSize) {
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
        messageInEventConsumer.accept(event);
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
