package com.wickr.java.model;

public enum MessageType {

    // https://wickrinc.github.io/wickrio-docs/#definitions

    TEXT_MESSAGE(1000, "Text Message"),
    VERIFICATION(3000, "Verification Message"),
    FILE_TRANSFER(6000, "File Transfer"),
    CALLING_MESSAGE(7000, "Calling Message"),
    LOCATION_MESSAGE(8000, "Location Message"),
    EDIT_MESSAGE(9000, "Edit Message"),
    CREATE_ROOM(4001, "Create Room"),
    MODIFY_ROOM(4002, "Modify Room"),
    MODIFY_ROOM_MEMBERS(4002, "Modify Room Members"),
    MODIFY_ROOM_PARAMETERS(4004, "Modify Room Parameters"),
    LEAVE_ROOM(4003, "Leave Room"),
    DELETE_ROOM(4005, "Delete Room"),
    DELETE_MESSAGE(4011, "Delete Message"),
    MESSAGE_ATTRIBUTES(4012, "Message Attributes");

    public static MessageType find(final Number type) {
        if (null == type || type.intValue() <= 0) {
            return null;
        }
        for (final MessageType messageType : MessageType.values()) {
            if (messageType.code == type.intValue()) {
                return messageType;
            }
        }
        return null;
    }

    private final int code;

    private final String text;

    MessageType(final int codeValue, final String textValue) {
        this.code = codeValue;
        this.text = textValue;
    }

    public int getCode() {
        return this.code;
    }

    public String getText() {
        return this.text;
    }
}
