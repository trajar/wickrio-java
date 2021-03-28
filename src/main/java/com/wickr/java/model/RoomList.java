package com.wickr.java.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class RoomList {
    @JsonProperty("rooms")
    private List<Room> rooms;

    @Deprecated
    public RoomList() {

    }

    public List<Room> getRooms() {
        if (null == this.rooms || this.rooms.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(this.rooms);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoomList roomList = (RoomList) o;
        return Objects.equals(rooms, roomList.rooms);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rooms);
    }
}
