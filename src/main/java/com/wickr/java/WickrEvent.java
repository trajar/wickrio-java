package com.wickr.java;

import com.wickr.java.model.Message;

import java.util.Objects;

/**
 * a message event
 *
 * @date 3/13/21.
 */
public class WickrEvent {
    private final WickrBot bot;

    private final Message message;

    public WickrEvent(final WickrBot bot, final Message message) {
        this.bot = bot;
        this.message = message;
    }

    public WickrBot getBot() {
        return bot;
    }

    public Message getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WickrEvent that = (WickrEvent) o;
        return Objects.equals(bot, that.bot) && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bot, message);
    }
}
