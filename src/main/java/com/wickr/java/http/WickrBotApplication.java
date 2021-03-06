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

package com.wickr.java.http;

import com.wickr.java.WickrBot;
import com.wickr.java.WickrListener;
import com.wickr.java.model.Message;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * a callback reset-ful server for wickr
 *
 * @date 2/28/21.
 */
public class WickrBotApplication extends Application {

    // the host callback url as visible from docker
    private final String hostUrl;

    // the set of wickr-io bots managed by web application
    private final Map<String, WickrBot> userBots = new ConcurrentHashMap<>();

    // the worker for processing events, if we are using callback
    private final WickrListener eventWorker;

    private final boolean useEventCallback;

    private static final String EVENT_CALLBACK_ENDPOINT = "event_callback";

    private static final Logger logger = LoggerFactory.getLogger(WickrBotApplication.class);

    public WickrBotApplication(final String host, final WickrListener eventWorker, final boolean registerCallbackEndpoint) {
        this.hostUrl = host;
        this.eventWorker = eventWorker;
        this.useEventCallback = registerCallbackEndpoint;
    }

    public void putBot(final WickrBot bot) throws IOException {
        if (null == bot) {
            throw new IllegalArgumentException();
        }
        logger.debug("Registering bot [" + bot + "] ...");
        this.userBots.put(bot.getUser(), bot);
        try {
            if (this.useEventCallback) {
                bot.setEventCallback(new StringBuilder(this.hostUrl)
                        .append("/").append(bot.getUser())
                        .append("/").append(EVENT_CALLBACK_ENDPOINT).toString());
            } else {
                bot.clearEventCallback();
            }
        } catch (final Exception e) {
            throw new IOException("Unable to register event callback for bot [" + bot + "].", e);
        }
    }

    WickrBot findBot(final String user) {
        if (null == user || user.isBlank()) {
            return null;
        }
        return this.userBots.get(user);
    }

    public void notifyMessageReceived(final String username, final Message message) {
        final WickrBot bot = this.userBots.get(username);
        if (null == bot) {
            return;
        }
        this.eventWorker.messageReceived(bot, message);
    }

    @Override
    public synchronized Restlet createInboundRoot() {
        Router router = new Router(getContext());
        if (this.useEventCallback) {
            router.attach("/{bot}/" + EVENT_CALLBACK_ENDPOINT, WickrEventCallbackResource.class);
        }
        router.attach("/{bot}/statistics", WickrStatisticsResource.class);
        router.attach("/{bot}/messages", WickrMessagesResource.class);
        router.attach("/{bot}/rooms", WickrRoomResource.class);
        router.attach("/{bot}/rooms/{room_id}", WickrRoomResource.class);
        router.attach("/{bot}/groups", WickrGroupResource.class);
        router.attach("/{bot}/groups/{group_id}", WickrGroupResource.class);
        return router;
    }
}
