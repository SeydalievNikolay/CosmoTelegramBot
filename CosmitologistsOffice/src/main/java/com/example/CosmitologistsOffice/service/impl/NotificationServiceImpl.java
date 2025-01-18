package com.example.CosmitologistsOffice.service.impl;

import com.example.CosmitologistsOffice.exceptions.ChatUserNotFoundException;
import com.example.CosmitologistsOffice.model.Appointment;
import com.example.CosmitologistsOffice.model.ChatUser;
import com.example.CosmitologistsOffice.model.Cosmetologist;
import com.example.CosmitologistsOffice.repository.ChatUserRepository;
import com.example.CosmitologistsOffice.service.BotLogicService;
import com.example.CosmitologistsOffice.service.NotificationService;
import com.example.CosmitologistsOffice.service.ServicePriceProvider;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
public class NotificationServiceImpl implements NotificationService {
    private final BotLogicService botLogicService;
    private final ChatUserRepository chatUserRepository;
    private final ServicePriceProvider servicePriceProvider;

    public NotificationServiceImpl(BotLogicService botLogicService, ChatUserRepository chatUserRepository, ServicePriceProvider servicePriceProvider) {
        this.botLogicService = botLogicService;
        this.chatUserRepository = chatUserRepository;
        this.servicePriceProvider = servicePriceProvider;
    }

    public void notifyCosmetologist(Appointment appointment) {
        log.debug("Начало уведомления косметолога для записи ID: {}", appointment.getId());

        Cosmetologist cosmetologist = appointment.getCosmetologist();
        if (cosmetologist == null) {
            log.warn("Косметолог не найден для записи ID: {}", appointment.getId());
            return;
        }

        ChatUser chatUser = chatUserRepository.findByChatId(appointment.getChatId())
                .orElseThrow(() -> new ChatUserNotFoundException("Пользователь не найден по chatId: " + appointment.getChatId()));

        BigDecimal servicePrice = BigDecimal.ZERO;
        String message = "";
        try {
            servicePrice = servicePriceProvider.getServicePrice(appointment.getService());
            message = formatNotificationMessage(appointment, chatUser, servicePrice);

            log.info("Отправка уведомления косметологу (ID: {}): {}", cosmetologist.getId(), message);

            botLogicService.sendSuccessMessage(cosmetologist.getTelegramChatId(), message);

        } catch (Exception e) {
            log.error("Ошибка при отправке уведомления для записи ID: {}. Ошибка: {}", appointment.getId(), e.getMessage(), e);
        }
    }


    private String formatNotificationMessage(Appointment appointment, ChatUser chatUser, BigDecimal servicePrice) {
        return "Новая запись:\n" +
                "Услуга: " + appointment.getService() + "\n" +
                "Цена: " + servicePrice + " руб.\n" +
                "Дата: " + appointment.getDate() + "\n" +
                "Время: " + appointment.getTime() + "\n" +
                "Клиент: " + chatUser.getFirstName() + " " + chatUser.getLastName() + "\n" +
                "Телефон клиента: " + chatUser.getUsername();
    }
}

