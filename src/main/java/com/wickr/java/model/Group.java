package com.wickr.java.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;
import java.util.stream.Collectors;

public class Group {

    public static Group createGroup(final Collection<String> members) {
        return createGroup(members, 0, 0);
    }

    public static Group createGroup(final Collection<String> members, final int ttl, final int bor) {
        final Set<String> uniqueMembers = new TreeSet<>(members);
        return new Group(uniqueMembers.stream().map(User::new).collect(Collectors.toList()), ttl, bor);
    }

    @JsonProperty("vgroupid")
    private String groupId;

    @JsonProperty("ttl")
    private Integer timeToLiveSeconds;

    @JsonProperty("bor")
    private Integer burnOnReadSeconds;

    @JsonProperty("members")
    private List<User> members = new ArrayList<>();

    protected Group(final Collection<User> members, final int ttl, final int bor) {
        this.members = new ArrayList<>(members);
        this.timeToLiveSeconds = ttl > 0 ? ttl : null;
        this.burnOnReadSeconds = bor > 0 ? bor : null;
    }

    @Deprecated
    public Group() {

    }

    public String getId() {
        return groupId;
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

    @Override
    public String toString() {
        return this.groupId != null ? this.groupId : "<new group>";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Group group = (Group) o;
        return Objects.equals(groupId, group.groupId) && Objects.equals(timeToLiveSeconds, group.timeToLiveSeconds) && Objects.equals(burnOnReadSeconds, group.burnOnReadSeconds) && Objects.equals(members, group.members);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, timeToLiveSeconds, burnOnReadSeconds, members);
    }
}
