package com.wickr.java.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Attachment {
    @JsonProperty("url")
    private String url;

    @JsonProperty("displayname")
    private String displayName;

    public Attachment(final String pathOrUrl, final String display) {
        this.url = pathOrUrl;
        this.displayName = display;
    }

    @Deprecated
    public Attachment() {

    }

    public String getUrl() {
        return url;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Attachment that = (Attachment) o;
        return Objects.equals(url, that.url) && Objects.equals(displayName, that.displayName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, displayName);
    }
}
