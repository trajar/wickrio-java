package com.wickr.java.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Statistics {
    public static Statistics createEmpty() {
        return new Statistics();
    }

    @JsonProperty("message_count")
    private Integer messageCount;

    @JsonProperty("pending_messages")
    private Integer pendingMessages;

    @JsonProperty("pending_callback_messages")
    private Integer pendingCallbackMessages;

    @JsonProperty("sent")
    private Integer sentCount;

    @JsonProperty("received")
    private Integer receivedCount;

    @JsonProperty("sent_errors")
    private Integer sentErrorsCount;

    @JsonProperty("recv_errors")
    private Integer receivedErrorsCount;

    @Deprecated
    public Statistics() {

    }

    public boolean isEmpty() {
        if (this.messageCount != null ||
            this.pendingMessages != null ||
            this.pendingCallbackMessages != null ||
            this.sentCount != null ||
            this.receivedCount != null ||
            this.sentErrorsCount != null ||
            this.receivedErrorsCount != null) {
            return false;
        } else {
            return true;
        }
    }

    public boolean hasErrors() {
        return this.getSentErrorsCount() > 0 || this.getReceivedErrorsCount() > 0;
    }

    public int getMessageCount() {
        return messageCount != null ? messageCount : 0;
    }

    public int getPendingMessages() {
        return pendingMessages != null ? pendingMessages : 0;
    }

    public int getPendingCallbackMessages() {
        return pendingCallbackMessages != null ? pendingCallbackMessages : 0;
    }

    public int getSentCount() {
        return sentCount != null ? sentCount : 0;
    }

    public int getReceivedCount() {
        return receivedCount != null ? receivedCount : 0;
    }

    public int getSentErrorsCount() {
        return sentErrorsCount != null ? sentErrorsCount : 0;
    }

    public int getReceivedErrorsCount() {
        return receivedErrorsCount != null ? receivedErrorsCount : 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Statistics that = (Statistics) o;
        return Objects.equals(messageCount, that.messageCount) && Objects.equals(pendingMessages, that.pendingMessages) && Objects.equals(pendingCallbackMessages, that.pendingCallbackMessages) && Objects.equals(sentCount, that.sentCount) && Objects.equals(receivedCount, that.receivedCount) && Objects.equals(sentErrorsCount, that.sentErrorsCount) && Objects.equals(receivedErrorsCount, that.receivedErrorsCount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageCount, pendingMessages, pendingCallbackMessages, sentCount, receivedCount, sentErrorsCount, receivedErrorsCount);
    }
}
