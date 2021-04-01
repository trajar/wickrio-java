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
