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
