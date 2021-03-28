package com.wickr.java.http;

import com.wickr.java.model.Message;
import com.wickr.java.util.JsonUtils;
import org.restlet.data.Status;
import org.restlet.resource.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * a callback restful server for wickr
 * <p>
 * https://wickrinc.github.io/wickrio-docs/#existing-integrations-broadcastbot-integration-broadcastbot-rest-api
 *
 * @date 2/28/21.
 */
public class WickrEventCallbackResource extends WickrResource {
    private static final Logger logger = LoggerFactory.getLogger(WickrEventCallbackResource.class);

    @Post("string:string")
    public String notifyPosted(final String json) throws IOException {
        if (null == json || json.isBlank()) {
            return "{}";
        }
        final Message message = JsonUtils.toEntity(json, Message.class);
        logger.debug("Event callback with message [" + message.getId() + "], type [" + message.getMessageType() + "].");
        return this.processEvent(message);
    }

    private String processEvent(final Message message) {
        if (null == message) {
            this.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return JsonUtils.errorMessage("Unexpected empty message.");
        }

        final String botname = this.getBotName();
        this.getApplication().notifyMessageReceived(this.getBotName(), message);

        final Map<String, Object> map = new HashMap<>(2);
        map.put("bot_user", botname);
        map.put("message_id", message.getId());
        return JsonUtils.fromMap(map);
    }
}
