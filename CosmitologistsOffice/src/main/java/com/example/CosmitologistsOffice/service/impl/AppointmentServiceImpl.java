package com.example.CosmitologistsOffice.service.impl;

import com.example.CosmitologistsOffice.model.Appointment;
import com.example.CosmitologistsOffice.repository.AppointmentRepository;
import com.example.CosmitologistsOffice.service.AppointmentService;
import lombok.extern.slf4j.Slf4j;

/**
 * Сервис для работы с записями пользователей.
 * Этот класс предоставляет методы для получения и создания записей для пользователей.
 * Используется для управления записями на услуги в системе.
 */
@Slf4j
public class AppointmentServiceImpl implements AppointmentService {
    private final AppointmentRepository appointmentRepository;


    public AppointmentServiceImpl(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;

    }

    /**
     * Получает запись для пользователя по chatId или создает новую, если она не найдена.
     * Метод проверяет наличие записи в базе данных по chatId. Если запись не найдена, создается новая запись
     * и сохраняется в базе данных.
     *
     * @param chatId Идентификатор чата пользователя.
     * @return Запись (Appointment) для данного chatId.
     */
    @Override
    public Appointment getOrCreateAppointment(long chatId) {
        Appointment appointment = appointmentRepository.findByChatId(chatId);
        if (appointment == null) {
            log.info("Запись для chatId {} не найдена. Создаём новую запись.", chatId);
            appointment = new Appointment();
            appointment.setChatId(chatId);
            appointmentRepository.save(appointment);
        }
        return appointment;
    }
}
