package com.example.CosmitologistsOffice.service;

import com.example.CosmitologistsOffice.model.Appointment;

public interface AppointmentService {

    Appointment getOrCreateAppointment(long chatId);

}
