package com.wickr.java;

import com.wickr.java.model.Message;

/**
 * a consumer for bot events
 *
 * @date 3/12/21.
 */
public interface WickrListener {
    void messageReceived(WickrBot bot, Message message);
}
