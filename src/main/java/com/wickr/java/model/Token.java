package com.wickr.java.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Token {

    @JsonProperty
    private String name;

    @JsonProperty
    private String value;

    public Token() {

    }

    public Token(final String name, final String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Token token = (Token) o;
        return Objects.equals(name, token.name) && Objects.equals(value, token.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }
}
