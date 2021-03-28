package com.wickr.java.http;

import com.wickr.java.WickrBot;
import com.wickr.java.model.Statistics;
import com.wickr.java.util.JsonUtils;
import org.restlet.data.Status;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WickrStatisticsResource extends WickrResource {
    private static final Logger logger = LoggerFactory.getLogger(WickrStatisticsResource.class);

    @Get("json")
    public String getStatistics() throws Exception {
        final WickrBot bot = this.ensureBot();
        final Statistics stats = bot.getStatistics();
        if (null == stats) {
            this.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return JsonUtils.errorMessage("Unable to get statistics for bot " + bot.getUser() + ".");
        } else {
            return JsonUtils.fromEntity(stats);
        }
    }

    @Delete("json")
    public String clearStatistics() throws Exception {
        final WickrBot bot = this.ensureBot();
        bot.clearStatistics();
        return JsonUtils.successMessage("Cleared statistics for " + bot.getUser() + "].");
    }
}
