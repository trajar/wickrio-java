package com.wickr.java.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.*;

public class Message {

    public static Message createDirectMessage(final String message, final String... recipients) {
        return createDirectMessage(message, Arrays.asList(recipients));
    }

    public static Message createDirectMessage(final String message, final Collection<String> recipients) {
        return new Message(message, User.createUsers(recipients), null);
    }

    public static Message createGroupMessage(final String message, final String groupId) {
        return new Message(message, null, groupId);
    }

    public static Message createDirectMessage(final Attachment attachment, final String... recipients) {
        return createDirectMessage(attachment, Arrays.asList(recipients));
    }

    public static Message createDirectMessage(final Attachment attachment, final Collection<String> recipients) {
        return new Message(attachment, User.createUsers(recipients), null);
    }

    public static Message createGroupMessage(final Attachment attachment, final String groupId) {
        return new Message(attachment, null, groupId);
    }

    @JsonProperty("message_id")
    private String messageId;

    @JsonProperty("message")
    private String message;

    @JsonProperty("attachment")
    private Attachment attachment;

    // time the message was sent, accurate to the microsecond
    @JsonProperty("msg_ts")
    private Number timeSentMsec;

    @JsonProperty("msgtype")
    private Number messageType;

    // id of the receiver, for 1-to-1 messages
    @JsonProperty("receiver")
    private String receiver;

    @JsonProperty("sender")
    private String sender;

    // displayable time message was sent
    @JsonProperty("time")
    private String displayableTimeSent;

    @JsonProperty("ttl")
    private String displayableTimeToLive;

    @JsonProperty("vgroupid")
    private String groupId;

    @JsonProperty("users")
    private List<User> users = new ArrayList<>();

    // set of control messages, typically for create/edit/delete groups
    @JsonProperty("control")
    private Map<String, Object> control;

    protected Message(final String message, final Collection<User> recipients, final String group) {
        this.users = recipients != null ? new ArrayList<>(recipients) : Collections.emptyList();
        this.groupId = group;
        this.message = message;
    }

    protected Message(final Attachment file, final Collection<User> recipients, final String group) {
        this.users = recipients != null ? new ArrayList<>(recipients) : Collections.emptyList();
        this.groupId = group;
        this.attachment = file;
    }

    @Deprecated
    public Message() {

    }

    public String getId() {
        return messageId;
    }

    public String getMessage() {
        return message;
    }

    public boolean isText() {
        if (null == this.messageType) {
            return false;
        }
        return MessageType.TEXT_MESSAGE.getCode() == this.messageType.intValue();
    }

    public Instant getTimeSent() {
        if (null == this.timeSentMsec) {
            return null;
        }
        return Instant.ofEpochMilli(this.timeSentMsec.longValue());
    }

    public boolean hasControl() {
        return null != this.control && !this.control.isEmpty();
    }

    public Number getTimeSentMilliseconds() {
        return timeSentMsec;
    }

    public MessageType getMessageType() {
        return MessageType.find(this.messageType);
    }

    public String getReceiver() {
        return receiver;
    }

    public String getSender() {
        return sender;
    }

    public String getDisplayableTimeSent() {
        return displayableTimeSent;
    }

    public String getDisplayableTimeToLive() {
        return displayableTimeToLive;
    }

    public String getGroupId() {
        return groupId;
    }

    public List<User> getUsers() {
        if (null == this.users || this.users.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(this.users);
    }

    @Override
    public String toString() {
        if (this.messageId != null) {
            return this.messageId + " " + this.getMessageType() + " from " + this.sender;
        } else {
            return "<new message> from " + this.sender;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message1 = (Message) o;
        return Objects.equals(messageId, message1.messageId) && Objects.equals(message, message1.message) && Objects.equals(attachment, message1.attachment) && Objects.equals(timeSentMsec, message1.timeSentMsec) && Objects.equals(messageType, message1.messageType) && Objects.equals(receiver, message1.receiver) && Objects.equals(sender, message1.sender) && Objects.equals(displayableTimeSent, message1.displayableTimeSent) && Objects.equals(displayableTimeToLive, message1.displayableTimeToLive) && Objects.equals(groupId, message1.groupId) && Objects.equals(users, message1.users) && Objects.equals(control, message1.control);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId, message, attachment, timeSentMsec, messageType, receiver, sender, displayableTimeSent, displayableTimeToLive, groupId, users, control);
    }
}
