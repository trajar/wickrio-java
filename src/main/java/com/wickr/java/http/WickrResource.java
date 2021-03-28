package com.wickr.java.http;

import com.wickr.java.WickrBot;
import com.wickr.java.util.ExceptionUtils;
import com.wickr.java.util.JsonUtils;
import com.wickr.java.util.StringUtils;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ServerResource;

import java.io.IOException;
import java.util.Arrays;

abstract public class WickrResource extends ServerResource {
    public WickrResource() {

    }

    @Override
    public void doCatch(final Throwable error) {
        if (ExceptionUtils.isException(error, IllegalStateException.class)) {
            final Status status = this.getStatus();
            if (status != null && Arrays.asList(
                    Status.CLIENT_ERROR_BAD_REQUEST,
                    Status.SERVER_ERROR_INTERNAL).contains(status)) {
                // skip as we already handled response status
                final Representation representation = this.getResponseEntity();
                if (null == representation) {
                    this.getResponse().setEntity(new StringRepresentation(JsonUtils.errorMessage("Unexpected error, please see log for details.")));
                }
                return;
            }
        }
        super.doCatch(error);
    }

    @Override
    public final WickrBotApplication getApplication() {
        return (WickrBotApplication) super.getApplication();
    }

    protected final WickrBot ensureBot() throws IOException {
        final String userBot = this.getBotName();

        if (null == userBot || userBot.isBlank()) {
            this.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            this.getResponse().setEntity(new StringRepresentation(JsonUtils.errorMessage("Invalid request, no user bot specified.")));
            throw new IllegalStateException();
        }

        final WickrBot bot = this.getApplication().findBot(userBot);
        if (null == bot) {
            this.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            this.getResponse().setEntity(new StringRepresentation(JsonUtils.errorMessage("Invalid request, unknown user bot [" + userBot + "].")));
            throw new IllegalStateException();
        }

        return bot;
    }

    public String getBotName() {
        return StringUtils.safeNull(this.getRequest().getAttributes().get("bot"));
    }
}
