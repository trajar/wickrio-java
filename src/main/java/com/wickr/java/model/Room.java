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

package com.wickr.java.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;
import java.util.stream.Collectors;

public class Room {

    public static Room createRoom(
            final String title, final String description,
            final Collection<String> members, final Collection<String> masters) {
        return createRoom(title, description, members, masters, 0, 0);
    }

    public static Room createRoom(
            final String title, final String description,
            final Collection<String> members, final Collection<String> masters,
            final int ttl, final int bor) {
        final Set<String> uniqueMembers = new TreeSet<>(members);
        final Set<String> uniqueMasters = new TreeSet<>(masters);
        return new Room(title, description,
                uniqueMembers.stream().map(User::new).collect(Collectors.toList()),
                uniqueMasters.stream().map(User::new).collect(Collectors.toList()),
                ttl, bor);
    }

    @JsonProperty("vgroupid")
    private String groupId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("ttl")
    private Integer timeToLiveSeconds;

    @JsonProperty("bor")
    private Integer burnOnReadSeconds;

    @JsonProperty("members")
    private List<User> members = new ArrayList<>();

    @JsonProperty("masters")
    private List<User> masters = new ArrayList<>();

    protected Room(final String title, final String description, final Collection<User> members, final Collection<User> masters, final int ttl, final int bor) {
        this.title = title;
        this.description = description;
        this.members = new ArrayList<>(members);
        this.masters = new ArrayList<>(masters);
        this.timeToLiveSeconds = ttl > 0 ? ttl : null;
        this.burnOnReadSeconds = bor > 0 ? bor : null;
    }

    @Deprecated
    public Room() {

    }

    public String getId() {
        return groupId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getTimeToLiveSeconds() {
        return this.timeToLiveSeconds != null ? this.timeToLiveSeconds : 0;
    }

    public boolean hasTimeToLive() {
        return this.timeToLiveSeconds != null && this.timeToLiveSeconds > 0;
    }

    public int getBurnOnReadSeconds() {
        return this.burnOnReadSeconds != null ? this.burnOnReadSeconds : 0;
    }

    public boolean hasBurnOnRead() {
        return this.burnOnReadSeconds != null && this.burnOnReadSeconds > 0;
    }

    public List<User> getMembers() {
        if (this.members == null || this.members.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(this.members);
    }

    public List<User> getMasters() {
        if (this.masters == null || this.masters.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(this.masters);
    }

    public boolean isMember(final User user) {
        if (null == user) {
            return false;
        }
        return this.isMember(user.getName());
    }

    public boolean isMember(final String username) {
        if (null == username || username.isBlank()) {
            return false;
        }
        for (final User user : this.getMembers()) {
            if (username.equalsIgnoreCase(user.getName())) {
                return true;
            }
        }
        return false;
    }

    public boolean isMaster(final User user) {
        if (null == user) {
            return false;
        }
        return this.isMaster(user.getName());
    }

    public boolean isMaster(final String username) {
        if (null == username || username.isBlank()) {
            return false;
        }
        for (final User user : this.getMasters()) {
            if (username.equalsIgnoreCase(user.getName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return this.groupId != null ? this.groupId : "<new room>";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Room room = (Room) o;
        return Objects.equals(groupId, room.groupId) && Objects.equals(title, room.title) && Objects.equals(description, room.description) && Objects.equals(timeToLiveSeconds, room.timeToLiveSeconds) && Objects.equals(burnOnReadSeconds, room.burnOnReadSeconds) && Objects.equals(members, room.members) && Objects.equals(masters, room.masters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, title, description, timeToLiveSeconds, burnOnReadSeconds, members, masters);
    }
}
