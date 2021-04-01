/*
 * MIT License
 *
 * Copyright (c) 2021
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

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
