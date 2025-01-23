package com.example.CosmitologistsOffice.service.impl;

import com.example.CosmitologistsOffice.exceptions.AppointmentNotFoundException;
import com.example.CosmitologistsOffice.exceptions.ChatUserNotFoundException;
import com.example.CosmitologistsOffice.model.Appointment;
import com.example.CosmitologistsOffice.model.ChatUser;
import com.example.CosmitologistsOffice.model.Cosmetologist;
import com.example.CosmitologistsOffice.repository.AppointmentRepository;
import com.example.CosmitologistsOffice.repository.ChatUserRepository;
import com.example.CosmitologistsOffice.repository.CosmetologistRepository;
import com.example.CosmitologistsOffice.service.BotLogicService;
import com.example.CosmitologistsOffice.service.NotificationService;
import com.example.CosmitologistsOffice.service.ServicePriceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;


/**
 * Этот сервис обрабатывает различные уведомления для косметологов,
 * такие как уведомления об отменах записей, новых записях и регистрациях пользователей.
 */
@Slf4j
@Transactional
public class NotificationServiceImpl implements NotificationService {
    private final BotLogicService botLogicService;
    private final ChatUserRepository chatUserRepository;
    private final ServicePriceProvider servicePriceProvider;
    private final CosmetologistRepository cosmetologistRepository;
    private final AppointmentRepository appointmentRepository;

    public NotificationServiceImpl(BotLogicService botLogicService, ChatUserRepository chatUserRepository, ServicePriceProvider servicePriceProvider, CosmetologistRepository cosmetologistRepository, AppointmentRepository appointmentRepository) {
        this.botLogicService = botLogicService;
        this.chatUserRepository = chatUserRepository;
        this.servicePriceProvider = servicePriceProvider;
        this.cosmetologistRepository = cosmetologistRepository;
        this.appointmentRepository = appointmentRepository;
    }

    /**
     * Отправляет уведомление косметологу об отмене записи.
     * Уведомление содержит информацию о пользователе, услуге, дате и времени записи.
     *
     * @param chatId Идентификатор чата пользователя, который отменил запись.
     * @param appointmentId Идентификатор записи, которая была отменена.
     */
    @Override
    public void notifyCosmetologistForCanselAppointment(long chatId,Long appointmentId) {
        log.debug("Отправка уведомления косметологу о отмене записи для chatId: {}", chatId);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new AppointmentNotFoundException("Запись не найдена "));

        ChatUser existingUser = chatUserRepository.findByChatId(chatId)
                .orElseThrow(() -> new ChatUserNotFoundException("Пользователь не найден по chatId: " + chatId));

        Cosmetologist cosmetologist = cosmetologistRepository.findByTelegramChatId(341641617L);

        if (cosmetologist == null) {
            log.error("Косметолог не найден для записи с chatId: {}", chatId);
            return;
        }

        String message = String.format("Запись с клиентом %s была отменена.\nУслуга: %s\nДата: %s\nВремя: %s",
                existingUser.getFirstName()+ " " +
                existingUser.getPhoneNumber(),
                appointment.getService(),
                appointment.getDate(),
                appointment.getTime());

        appointmentRepository.deleteById(appointmentId);
        try {
            botLogicService.sendSuccessMessage(cosmetologist.getTelegramChatId(), message);
            log.info("Уведомление косметологу отправлено: {}", message);
        } catch (Exception e) {
            log.error("Ошибка при отправке уведомления косметологу об отмене записи", e);
        }
    }

    /**
     * Отправляет уведомление косметологу о новой записи.
     * Уведомление включает информацию о услуге, цене, дате, времени и клиенте.
     *
     * @param appointment Объект записи, содержащий информацию о записи.
     */
    @Override
    public void notifyCosmetologist(Appointment appointment) {
        log.debug("Начало уведомления косметолога для записи ID: {}", appointment.getId());

        Cosmetologist cosmetologist = appointment.getCosmetologist();

        ChatUser chatUser = chatUserRepository.findByChatId(appointment.getChatId())
                .orElseThrow(() -> new ChatUserNotFoundException("Пользователь не найден по chatId: " + appointment.getChatId()));

        BigDecimal servicePrice;
        String message;
        try {
            servicePrice = servicePriceProvider.getServicePrice(appointment.getService());
            message = formatNotificationMessage(appointment, chatUser, servicePrice);

            log.info("Отправка уведомления косметологу (ID: {}): {}", cosmetologist.getId(), message);
            botLogicService.sendSuccessMessage(cosmetologist.getTelegramChatId(), message);


        } catch (Exception e) {
            log.error("Ошибка при отправке уведомления для записи ID: {}. Ошибка: {}", appointment.getId(), e.getMessage(), e);
        }
    }


    /**
     * Форматирует сообщение для уведомления косметолога о новой записи.
     *
     * @param appointment Объект записи, содержащий информацию о записи.
     * @param chatUser Объект пользователя, который записан на прием.
     * @param servicePrice Цена услуги.
     * @return Отформатированное сообщение для отправки косметологу.
     */
    private String formatNotificationMessage(Appointment appointment, ChatUser chatUser, BigDecimal servicePrice) {
        return "Новая запись:\n" +
                "Услуга: " + appointment.getService() + "\n" +
                "Цена: " + servicePrice + " руб.\n" +
                "Дата: " + appointment.getDate() + "\n" +
                "Время: " + appointment.getTime() + "\n" +
                "Клиент: " + chatUser.getFirstName() + " " + chatUser.getLastName() + "\n" +
                "Телефон клиента: " + chatUser.getPhoneNumber();
    }

    /**
     * Отправляет уведомление косметологу при регистрации нового пользователя.
     * Уведомление содержит номер телефона клиента.
     *
     * @param chatId Идентификатор чата пользователя.
     */
    @Override
    public void notifyingTheCosmetologistWhenRegisteringAUser(long chatId) {
        log.info("Начало уведомления косметолога при регистрации пользователя");

        Cosmetologist cosmetologist = cosmetologistRepository.findByTelegramChatId(341641617L);
        log.info("Косметолог найден в БД");

        ChatUser existingUser = chatUserRepository.findByChatId(chatId)
                .orElseThrow(() -> new ChatUserNotFoundException("Пользователь не найден по chatId"));
        try {
            String message = notificationFormatSentPhoneNumber(existingUser);

            log.info("Отправка уведомления косметологу (ID: {}): {}", cosmetologist.getName(), message);
            botLogicService.sendSuccessMessage(cosmetologist.getTelegramChatId(), message);

        } catch (Exception e) {
            log.error("Ошибка при отправке уведомления для записи ID: {}. Ошибка: {}", existingUser.getId(), e.getMessage(), e);
        }
    }

    /**
     * Форматирует сообщение, которое отправляется косметологу, когда клиент указал номер телефона.
     *
     * @param chatUser Объект пользователя, который указал номер телефона.
     * @return Отформатированное сообщение для отправки косметологу.
     */
    private String notificationFormatSentPhoneNumber(ChatUser chatUser) {
        return "Клиент указал номер для связи: " + chatUser.getPhoneNumber();
    }
}

