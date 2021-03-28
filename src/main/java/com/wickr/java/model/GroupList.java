package com.wickr.java.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class GroupList {
    @JsonProperty("groupconvos")
    private List<Group> groups;

    @Deprecated
    public GroupList() {

    }

    public List<Group> getGroups() {
        if (null == this.groups || this.groups.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(this.groups);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupList groupList = (GroupList) o;
        return Objects.equals(groups, groupList.groups);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groups);
    }
}
