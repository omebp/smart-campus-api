package com.smartcampus.exception;

public class LinkedResourceNotFoundException extends RuntimeException {

    private final String roomId;

    public LinkedResourceNotFoundException(String roomId) {
        super("The roomId referenced does not exist");
        this.roomId = roomId;
    }

    public String getRoomId() {
        return roomId;
    }
}
