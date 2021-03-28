package com.wickr.java.http;

import com.wickr.java.WickrBot;
import com.wickr.java.model.Message;
import com.wickr.java.util.JsonUtils;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WickrMessagesResource extends WickrResource {
    private static final Logger logger = LoggerFactory.getLogger(WickrMessagesResource.class);

    @Post("json:json")
    public String send(final String json) throws Exception {
        final WickrBot bot = this.ensureBot();
        final Message message = JsonUtils.toEntity(json, Message.class);
        if (bot.sendMessage(message)) {
            return JsonUtils.successMessage();
        } else {
            this.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return JsonUtils.errorMessage("Unable to send message from " + bot.getUser() + ".");
        }
    }

    @Get("json")
    public String retrieve() throws Exception {
        final String startValue = this.getQueryValue("start");
        int start = -1;
        if (startValue != null && !startValue.isBlank()) {
            start = Integer.parseInt(startValue);
        }
        final String countValue = this.getQueryValue("count");
        int count = -1;
        if (countValue != null && !countValue.isBlank()) {
            count = Integer.parseInt(countValue);
        }
        return JsonUtils.fromEntity(this.ensureBot().getUnreadMessages(start, count));
    }
}
