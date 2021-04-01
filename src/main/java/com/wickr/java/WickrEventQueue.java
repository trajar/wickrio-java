package com.wickr.java;

import com.wickr.java.model.Message;

import java.util.concurrent.TimeUnit;

/**
 * an event queue for processing wickr message events
 *
 * @date 3/12/21.
 */
public interface WickrEventQueue extends WickrComponent {
    default boolean add(WickrBot bot, Message message) {
        return this.add(new WickrEvent(bot, message));
    }

    boolean add(WickrEvent event);

    WickrEvent remove(int timeout, TimeUnit unit) throws InterruptedException;

    default WickrEvent remove() throws InterruptedException {
        return remove(-1, TimeUnit.MILLISECONDS);
    }

}
