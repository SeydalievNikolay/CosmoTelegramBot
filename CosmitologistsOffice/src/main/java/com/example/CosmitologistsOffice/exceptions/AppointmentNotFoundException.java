package com.example.CosmitologistsOffice.exceptions;

public class AppointmentNotFoundException extends RuntimeException{
    public AppointmentNotFoundException(String message) {
        super(message);
    }
}
