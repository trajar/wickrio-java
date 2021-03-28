package com.wickr.java.impl;

import com.wickr.java.WickrBot;
import com.wickr.java.WickrComponent;
import com.wickr.java.WickrEventQueue;
import com.wickr.java.WickrListener;
import com.wickr.java.model.Message;
import com.wickr.java.util.ExceptionUtils;
import org.apache.hc.client5.http.HttpHostConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * a worker that listens for events and publishes them to queue
 *
 * @date 3/18/21.
 */
public class WickrEventPublishingWorker implements Runnable, WickrComponent, WickrListener {

    public static WickrEventDelegateThread createAsThread(final Collection<WickrBot> botsToListenFor, final WickrEventQueue queue, final int queryFrequencyMsec) {
        final WickrEventDelegateThread thread = new WickrEventDelegateThread(new WickrEventPublishingWorker(botsToListenFor, queue, queryFrequencyMsec));
        thread.setName("WickIO-EventPublisher-Main");
        thread.setDaemon(true);
        return thread;
    }

    public static WickrEventPublishingWorker createAsApi(final Collection<WickrBot> botsToListenFor, final WickrEventQueue queue) {
        return new WickrEventPublishingWorker(botsToListenFor, queue, -1);
    }

    private static final Logger logger = LoggerFactory.getLogger(WickrEventPublishingWorker.class);

    private static final AtomicInteger threadCount = new AtomicInteger();

    private final WickrEventQueue queue;

    private final ExecutorService executor;

    private final int messageQueryFrequencyMsec;

    private final List<WickrBot> bots;

    private final AtomicBoolean closed = new AtomicBoolean(false);

    WickrEventPublishingWorker(final Collection<WickrBot> botsToListenFor, final WickrEventQueue queue, final int queryFrequencyMsec) {
        this.queue = queue;
        this.bots = new ArrayList<>(botsToListenFor);
        this.messageQueryFrequencyMsec = queryFrequencyMsec;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("WickIO-EventPublisher-Worker-" + threadCount.incrementAndGet());
            return t;
        });
    }

    @Override
    public void shutdown() throws Exception {
        this.closed.getAndSet(true);
        this.executor.shutdownNow();
    }

    public boolean isClosed() {
        return this.closed.get();
    }

    @Override
    public void messageReceived(final WickrBot bot, final Message message) {
        if (null == bot || null == message) {
            return;
        } else if (this.isClosed()) {
            return;
        }
        final Runnable worker = () -> {
            try {
                if (!attemptToAddMessageEvent(bot, message)) {
                    logger.warn("Unable to add message [" + message.getId() + "] to queue.");
                }
            } catch (final Exception e) {
                logger.warn("Error adding message [" + message.getId() + "] to queue.", e);
            }
        };
        this.executor.submit(worker);
    }

    private boolean attemptToAddMessageEvent(final WickrBot bot, final Message message) {
        if (this.isClosed()) {
            return false;
        }

        logger.info("Adding message [" + message.getId() + "] to event queue ...");
        if (queue.add(bot, message)) {
            return true;
        }

        boolean wasAdded = false;
        for (int i = 0; !wasAdded && i < 10; i++) {
            wasAdded = queue.add(bot, message);
            if (wasAdded) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void run() {
        if (this.messageQueryFrequencyMsec <= 0) {
            // not querying for messages, bail early
            return;
        }
        try {
            checkMessagesUntilClosed();
        } catch (final InterruptedException e) {
            logger.debug("Message query thread interrupted, existing.", e);
        } catch (final Exception e) {
            logger.warn("Found exception in message query thread, aborting.", e);
        }
    }

    private void checkMessagesUntilClosed() throws InterruptedException, HttpHostConnectException {
        while (!this.isClosed()) {
            Thread.sleep(this.messageQueryFrequencyMsec);
            final int numRead = checkForMessagesInThread();
            if (numRead > 0) {
                logger.trace("Processed [" + numRead + "] events in message query thread.");
            }
        }
    }

    private int checkForMessagesInThread() throws HttpHostConnectException {
        if (isClosed()) {
            return 0;
        }
        int numRead = 0;
        for (final WickrBot bot : this.bots) {
            if (isClosed()) {
                return numRead;
            }
            try {
                for (final Message msg : bot.getUnreadMessages()) {
                    if (isClosed()) {
                        return numRead;
                    }
                    this.messageReceived(bot, msg);
                    numRead++;
                }
            } catch (final IOException e) {
                if (ExceptionUtils.isException(e, HttpHostConnectException.class)) {
                    logger.error("Unable to query messages for bot [" + bot + "] - error connecting to host.", e);
                    throw new HttpHostConnectException("Unable to connect to host when querying messages for [" + bot + "].");
                } else {
                    logger.warn("Unable to query messages for bot [" + bot + "].", e);
                }
            }
        }
        return numRead;
    }
}
