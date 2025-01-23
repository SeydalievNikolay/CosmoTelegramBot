package com.example.CosmitologistsOffice.service;

import com.example.CosmitologistsOffice.model.Appointment;

public interface NotificationService {
    void notifyCosmetologistForCanselAppointment(long chatId,Long appointmentId);
    void notifyCosmetologist(Appointment appointment);
    void notifyingTheCosmetologistWhenRegisteringAUser(long chatId);

}
