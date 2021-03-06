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
import com.wickr.java.model.Room;
import com.wickr.java.util.JsonUtils;
import com.wickr.java.util.StringUtils;
import org.restlet.data.Status;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WickrRoomResource extends WickrResource {
    private static final Logger logger = LoggerFactory.getLogger(WickrRoomResource.class);

    @Get("json")
    public String retrieveRooms() throws Exception {
        final String roomId = StringUtils.safeNull(this.getRequest().getAttributes().get("room_id"));
        if (roomId != null && !roomId.isBlank()) {
            return JsonUtils.fromEntity(this.retrieveSingleRoom(roomId));
        } else {
            return JsonUtils.fromEntity(this.retrieveRoomList());
        }
    }

    private Room retrieveSingleRoom(final String roomId) throws IOException {
        return this.ensureBot().getRoom(roomId);
    }

    private List<Room> retrieveRoomList() throws IOException {
        return this.ensureBot().getRooms();
    }

    @Post("json:json")
    public String createRoom(final String json) throws Exception {
        final WickrBot bot = this.ensureBot();
        final Room room = JsonUtils.toEntity(json, Room.class);
        final String roomId = bot.createRoom(room);
        if (roomId != null && !roomId.isBlank()) {
            final Map<String, String> map = new LinkedHashMap<>(2);
            map.put("status", "success");
            map.put("room_id", roomId);
            return JsonUtils.fromMap(map);
        } else {
            this.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return JsonUtils.errorMessage("Unable to create room for " + bot.getUser() + ".");
        }
    }

    @Delete("json")
    public String deleteRoom() throws Exception {
        final String roomId = StringUtils.safeNull(this.getRequest().getAttributes().get("room_id"));
        if (null == roomId || roomId.isBlank()) {
            this.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return JsonUtils.errorMessage("Room id is empty.");
        }

        final WickrBot bot = this.ensureBot();
        final Room room = bot.getRoom(roomId);
        if (null == room) {
            this.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            return JsonUtils.errorMessage(bot.getUser() + " is not associated with room " + roomId + ".");
        }

        if (room.isMaster(bot.getUser())) {
            bot.deleteRoom(roomId);
            return JsonUtils.successMessage(bot.getUser() + " deleted room " + roomId + ".");
        } else {
            bot.leaveRoom(roomId);
            return JsonUtils.successMessage(bot.getUser() + " left room " + roomId + ".");
        }
    }
}
