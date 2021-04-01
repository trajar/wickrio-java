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
import com.wickr.java.model.Group;
import com.wickr.java.model.User;
import com.wickr.java.util.JsonUtils;
import com.wickr.java.util.StringUtils;
import org.restlet.data.Status;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WickrGroupResource extends WickrResource {
    private static final Logger logger = LoggerFactory.getLogger(WickrGroupResource.class);

    @Get("json")
    public String retrieveGroups() throws Exception {
        final String groupId = StringUtils.safeNull(this.getRequest().getAttributes().get("group_id"));
        if (groupId != null && !groupId.isBlank()) {
            return JsonUtils.fromEntity(this.retrieveSingleGroup(groupId));
        } else {
            return JsonUtils.fromEntity(this.retrieveGroupList());
        }
    }

    private Group retrieveSingleGroup(final String groupId) throws IOException {
        return this.ensureBot().getGroup(groupId);
    }

    private List<Group> retrieveGroupList() throws IOException {
        return this.ensureBot().getGroups();
    }

    @Post("json:json")
    public String createGroup(final String json) throws Exception {
        final WickrBot bot = this.ensureBot();

        // attempt to get list of users from json
        final List<String> users = new ArrayList<>();
        final String trimmedJson = StringUtils.safeNull(json).trim();
        if (trimmedJson.isEmpty() || trimmedJson.equals("[]") || trimmedJson.equals("{}")) {
            this.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return JsonUtils.errorMessage("User list is empty.");
        }
        try {
            final List<User> userList = JsonUtils.toEntityList(json, User.class);
            if (userList != null && !userList.isEmpty()) {
                final List<String> userNames = userList.stream()
                        .map(User::getName)
                        .filter((x) -> x != null && !x.isEmpty())
                        .collect(Collectors.toList());
                if (userNames.size() > 0) {
                    users.addAll(userNames);
                }
            }
        } catch (final Exception e) {
            logger.trace("Unable to parse json as list of user elements, trying string list.", e);
        }
        if (users.isEmpty()) {
            try {
                final List<?> elements = JsonUtils.toEntity(json, List.class);
                final List<String> userNames = elements.stream()
                        .map((x) -> x != null ? x.toString() : "")
                        .filter((x) -> !x.isBlank())
                        .collect(Collectors.toList());
                if (userNames.size() > 0) {
                    users.addAll(userNames);
                }
            } catch (final Exception e) {
                logger.trace("Unable to parse json as list of string user names.", e);
            }
        }
        if (users.isEmpty()) {
            this.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return JsonUtils.errorMessage("Unable to create group with empty or unknown user list.");
        }
        final String groupId = bot.createGroup(users);
        if (groupId != null && !groupId.isBlank()) {
            final Map<String, String> map = new LinkedHashMap<>(2);
            map.put("status", "success");
            map.put("group_id", groupId);
            return JsonUtils.fromMap(map);
        } else {
            this.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return JsonUtils.errorMessage("Unable to create group conversation for " + bot.getUser() + ".");
        }
    }

    @Delete("json")
    public String deleteGroup() throws Exception {
        final String groupId = StringUtils.safeNull(this.getRequest().getAttributes().get("group_id"));
        if (null == groupId || groupId.isBlank()) {
            this.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return JsonUtils.errorMessage("Group id is empty.");
        }

        final WickrBot bot = this.ensureBot();
        final Group group = bot.getGroup(groupId);
        if (null == group) {
            this.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            return JsonUtils.errorMessage(bot.getUser() + " is not associated with group conversation " + groupId + ".");
        }

        bot.deleteGroup(groupId);
        return JsonUtils.successMessage(bot.getUser() + " deleted group conversation " + groupId + ".");
    }
}
