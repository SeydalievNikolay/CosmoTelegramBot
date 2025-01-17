package com.example.CosmitologistsOffice.service.impl;

import com.example.CosmitologistsOffice.exceptions.ChatUserNotFoundException;
import com.example.CosmitologistsOffice.model.Appointment;
import com.example.CosmitologistsOffice.model.ChatUser;
import com.example.CosmitologistsOffice.model.Cosmetologist;
import com.example.CosmitologistsOffice.repository.AppointmentRepository;
import com.example.CosmitologistsOffice.repository.ChatUserRepository;
import com.example.CosmitologistsOffice.repository.CosmetologistRepository;
import com.example.CosmitologistsOffice.repository.ServiceRepository;
import com.example.CosmitologistsOffice.service.AppointmentService;
import com.example.CosmitologistsOffice.service.SendMessageForUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

@Slf4j
@Service
public class AppointmentServiceImpl implements AppointmentService {
    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private SendMessageForUserService sendMessageForUserService;
    @Autowired
    private CosmetologistRepository cosmetologistRepository;
    @Autowired
    private ServiceRepository serviceRepository;
    @Autowired
    private ChatUserRepository chatUserRepository;

    // Выбор записи по chatId (например, для отображения или изменений)
    public Optional<Appointment> getAppointmentByChatId(Long chatId) {
        return appointmentRepository.findById(chatId);
    }

    @Override
    public boolean isBooked(Appointment appointment) {
        return appointment != null && appointment.getBookedAt() != null;
    }

    public Appointment getAppointment(long chatId) {
        Appointment appointment = appointmentRepository.findByChatId(chatId);
        if (appointment == null) {
            appointment = new Appointment();
            appointment.setChatId(chatId);
        } else {
            sendMessageForUserService.sendErrorMessage(chatId, "У вас нет активной записи для подтверждения.");
        }
        return appointment;
    }

    public Appointment createNewAppointment(Cosmetologist cosmetologist, ChatUser chatUser) {
        Appointment appointment = new Appointment();
        appointment.setCosmetologist(cosmetologist);
        appointment.setChatId(chatUser.getId());
        appointment.setService("");
        appointment.setDate("");
        appointment.setTime("");
        appointment.setBooked(false);
        appointment.setChatUser(chatUser);

        return appointmentRepository.save(appointment);
    }

    public void updateService(long chatId, String serviceName) {
        Appointment appointment = getAppointment(chatId);
        if (appointment != null) {
            appointment.setService(serviceName);
            appointmentRepository.save(appointment);
        }
    }

    public void updateTime(long chatId, String serviceName, LocalTime time) {
        Appointment appointment = getAppointment(chatId);
        if (appointment != null && appointment.getService() != null && appointment.getService().equals(serviceName)) {
            appointment.setTime(time.toString());
            appointmentRepository.save(appointment);
        }
    }

    public void updateDate(long chatId, String serviceName, LocalDate date) {
        Appointment appointment = getAppointment(chatId);
        if (appointment != null && appointment.getService() != null && appointment.getService().equals(serviceName)) {
            appointment.setDate(date.toString());
            appointmentRepository.save(appointment);
        }
    }


    public void saveAppointment(Long chatId, String selectedService, String selectedDate, String selectedTime) {
        Appointment appointment = getAppointment(chatId);
        if (appointment != null) {
            appointment.setService(selectedService);
            appointment.setDate(selectedDate);
            appointment.setTime(selectedTime);
            appointment.setBooked(true);
            LocalDateTime now = LocalDateTime.now();
            Timestamp timestampNow = Timestamp.valueOf(now);
            appointment.setBookedAt(timestampNow);
            appointmentRepository.save(appointment);
        }
    }

    public Appointment getOrCreateAppointment(long chatId) {
        Appointment appointment = appointmentRepository.findByChatId(chatId);
        if (appointment == null) {
            appointment = new Appointment();
            appointment.setChatId(chatId);
            appointmentRepository.save(appointment);
        }
        return appointment;
    }

    public void recordAppointment(long chatId) {
        Appointment existingAppointment = appointmentRepository.findByChatId(chatId);
        if (existingAppointment == null) {
            Optional<ChatUser> optionalChatUser = chatUserRepository.findByChatId(chatId);
            if (optionalChatUser.isPresent()) {
                ChatUser chatUser = optionalChatUser.get();
                Cosmetologist cosmetologist = cosmetologistRepository.findByTelegramChatId(chatId);

                Appointment newAppointment = createNewAppointment(cosmetologist, chatUser);

                appointmentRepository.save(newAppointment);
            } else {
                log.warn("Пользователь не найден для chatId: {}", chatId);
                throw new ChatUserNotFoundException("Пользователь не найден");
            }
        } else {
            updateExistingAppointment(existingAppointment, chatId);
        }
    }

    private void updateExistingAppointment(Appointment existingAppointment, long chatId) {
        String selectedService = serviceRepository.findByName("service");
        String selectedDate = LocalDateTime.now().plusDays(7).toString();
        String selectedTime = LocalTime.of(10, 0).toString();

        existingAppointment.setService(selectedService);
        existingAppointment.setDate(selectedDate);
        existingAppointment.setTime(selectedTime);

        existingAppointment.setBooked(true);

        LocalDateTime now = LocalDateTime.now();
        Timestamp timestampNow = Timestamp.valueOf(now);
        existingAppointment.setBookedAt(timestampNow);

        appointmentRepository.save(existingAppointment);
    }


}
