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
        return this.messageCount == null &&
                this.pendingMessages == null &&
                this.pendingCallbackMessages == null &&
                this.sentCount == null &&
                this.receivedCount == null &&
                this.sentErrorsCount == null &&
                this.receivedErrorsCount == null;
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
