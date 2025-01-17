package com.example.CosmitologistsOffice.exceptions;

public class ChatUserNotFoundException extends RuntimeException {
    public ChatUserNotFoundException(String message) {
        super(message);
    }

    public ChatUserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
