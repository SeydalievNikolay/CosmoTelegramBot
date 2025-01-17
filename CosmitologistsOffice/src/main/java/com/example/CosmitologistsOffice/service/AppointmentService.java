package com.example.CosmitologistsOffice.service;

import com.example.CosmitologistsOffice.model.Appointment;
import com.example.CosmitologistsOffice.model.ChatUser;
import com.example.CosmitologistsOffice.model.Cosmetologist;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

public interface AppointmentService {
    void saveAppointment(Long chatId, String selectedService, String selectedDate, String selectedTime);
    Optional<Appointment> getAppointmentByChatId(Long chatId);
    boolean isBooked(Appointment appointment);

    Appointment getAppointment(long chatId);
    Appointment createNewAppointment(Cosmetologist cosmetologist, ChatUser chatUser);
    void updateService(long chatId, String serviceName);
    void updateTime(long chatId, String serviceName, LocalTime time);
    void updateDate(long chatId, String serviceName, LocalDate date);
    Appointment getOrCreateAppointment(long chatId);

    void recordAppointment(long chatId);
}
