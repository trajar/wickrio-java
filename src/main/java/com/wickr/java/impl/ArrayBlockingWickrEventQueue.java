package com.wickr.java.impl;

import com.wickr.java.WickrEvent;
import com.wickr.java.WickrEventQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ArrayBlockingWickrEventQueue implements WickrEventQueue {
    private static final Logger logger = LoggerFactory.getLogger(ArrayBlockingWickrEventQueue.class);

    private final BlockingQueue<WickrEvent> queue;

    private final ExecutorService executor;

    public ArrayBlockingWickrEventQueue() {
        this(256);
    }

    public ArrayBlockingWickrEventQueue(final int queueSize) {
        this.queue = new ArrayBlockingQueue<>(queueSize);
        this.executor = createSingleThreadedExecutor();
    }

    @Override
    public boolean add(final WickrEvent event) {
        // attempt to add immediately
        if (this.queue.offer(event)) {
            return true;
        }
        // if unable to add now, attempt to add later
        final Runnable worker = () -> {
            try {
                queue.put(event);
            } catch (final Exception e) {
                logger.warn("Unable to add event to queue.", e);
            }
        };
        if (this.executor.isShutdown()) {
            throw new IllegalStateException();
        }
        try {
            this.executor.submit(worker);
            return true;
        } catch (final Exception e) {
            logger.warn("Unable to submit worker event to executor.", e);
            return false;
        }
    }

    @Override
    public WickrEvent remove(final int timeout, final TimeUnit unit) throws InterruptedException {
        if (timeout > 0) {
            return this.queue.poll(timeout, unit);
        } else {
            return this.queue.take();
        }
    }

    @Override
    public void shutdown() {
        if (!this.executor.isShutdown()) {
            this.executor.shutdownNow();
        }
        this.queue.clear();
    }

    private static final AtomicInteger threadCount = new AtomicInteger();

    private static ExecutorService createSingleThreadedExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName(ArrayBlockingWickrEventQueue.class.getSimpleName() + "-Worker-" + threadCount.incrementAndGet());
            return t;
        });
    }
}
