package com.wickr.java.impl;

import com.wickr.java.WickrBot;
import com.wickr.java.WickrComponent;
import com.wickr.java.WickrListener;
import com.wickr.java.model.Message;

/**
 * a thread which delegates to existing publishing worker
 *
 * @date 3/18/21.
 */
public class WickrEventDelegateThread extends Thread implements WickrListener, WickrComponent {
    private final WickrEventPublishingWorker worker;

    WickrEventDelegateThread(final WickrEventPublishingWorker worker) {
        this.worker = worker;
    }

    @Override
    public void run() {
        this.worker.run();
    }

    @Override
    public void shutdown() throws Exception {
        this.worker.shutdown();
        this.interrupt();
    }

    @Override
    public void messageReceived(WickrBot bot, Message message) {
        this.worker.messageReceived(bot, message);
    }
}
