package com.example.CosmitologistsOffice.service.impl;

import com.example.CosmitologistsOffice.exceptions.ChatUserNotFoundException;
import com.example.CosmitologistsOffice.model.Appointment;
import com.example.CosmitologistsOffice.model.ChatUser;
import com.example.CosmitologistsOffice.model.Cosmetologist;
import com.example.CosmitologistsOffice.repository.ChatUserRepository;
import com.example.CosmitologistsOffice.service.NotificationService;
import com.example.CosmitologistsOffice.service.SendMessageForUserService;
import com.example.CosmitologistsOffice.service.ServicePriceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {
    @Autowired
    private SendMessageForUserService sendMessageForUserService;
    @Autowired
    private ChatUserRepository chatUserRepository;
    @Autowired
    private ServicePriceProvider servicePriceProvider;

    public void notifyCosmetologist(Appointment appointment) {
        log.debug("Начало уведомления косметолога для записи ID: {}", appointment.getId());

        Cosmetologist cosmetologist = appointment.getCosmetologist();
        if (cosmetologist == null) {
            log.warn("Косметолог не найден для записи ID: {}", appointment.getId());
            return;
        }

        ChatUser chatUser = chatUserRepository.findByChatId(appointment.getChatId())
                .orElseThrow(() -> new ChatUserNotFoundException("ChatUser not found for chatId: " + appointment.getChatId()));

        try {
            BigDecimal servicePrice = servicePriceProvider.getServicePrice(appointment.getService());

            String message = formatNotificationMessage(appointment, chatUser, servicePrice);

            log.info("Отправка уведомления косметологу (ID: {}): {}", cosmetologist.getId(), message);

            sendMessageForUserService.sendSuccessMessage(cosmetologist.getTelegramChatId(), message);

            sendSMSNotificationIfRequired(cosmetologist, message);
        } catch (Exception e) {
            log.error("Ошибка при отправке уведомления для записи ID: {}. Текст сообщения: {}", appointment.getId(),
                    formatNotificationMessage(appointment, chatUser, servicePriceProvider.getServicePrice(appointment.getService())), e);
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

    private void sendSMSNotificationIfRequired(Cosmetologist cosmetologist, String message) {
        String notificationNumber = getNotificationNumber(cosmetologist);
        if (notificationNumber != null) {
            log.info("Попытка отправки уведомления по SMS для косметолога ID: {}, номер: {}",
                    cosmetologist.getId(), notificationNumber);
            // Пример интеграции с SMS-поставщиком
            // Twilio.sendMessage(notificationNumber, message);
        } else {
            log.warn("Номер телефона или никнейм не найден для косметолога ID: {}", cosmetologist.getId());
        }
    }
    private String getNotificationNumber(Cosmetologist cosmetologist) {
        if (cosmetologist.getPhone() != null && !cosmetologist.getPhone().isEmpty()) {
            return cosmetologist.getPhone();
        } else if (cosmetologist.getNickName() != null && !cosmetologist.getNickName().isEmpty()) {
            return "@" + cosmetologist.getNickName();
        }
        return null;
    }

    private void sendNotification(String number, String message) {
        log.info("Попытка отправки уведомления по номеру: {}", number);
        // Пример интеграции с SMS-поставщиком
        // Twilio.sendMessage(number, message);
    }
}

