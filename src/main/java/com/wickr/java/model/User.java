package com.wickr.java.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

public class User implements Comparable<User> {

    public static User createUser(final String username) {
        if (null == username || username.isEmpty()) {
            return null;
        }
        return new User(username);
    }

    public static Collection<User> createUsers(final Collection<String> usernames) {
        if (null == usernames || usernames.isEmpty()) {
            return null;
        }
        return usernames.stream().map(User::createUser).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @JsonProperty("name")
    private String name;

    public User(final String username) {
        this.name = username;
    }

    @Deprecated
    public User() {

    }

    public String getName() {
        return this.name;
    }


    @Override
    public int compareTo(final User other) {
        if (null == other) {
            return 1;
        } else if (this.name == null) {
            return other.name == null ? 0 : -1;
        } else if (other.name == null) {
            return 1;
        } else {
            return this.name.compareToIgnoreCase(other.name);
        }
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(name, user.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
