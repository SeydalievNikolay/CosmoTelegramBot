package com.example.CosmitologistsOffice.service.impl;

import com.example.CosmitologistsOffice.service.MessageSender;
import com.example.CosmitologistsOffice.service.TelegramBotService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Реализация интерфейса {@link MessageSender} для отправки и редактирования сообщений через Telegram Bot API.
 * Этот класс использует {@link TelegramBotService} для выполнения запросов к Telegram Bot API.
 */
public class TelegramMessageSender implements MessageSender {
    private final TelegramBotService telegramBotService;

    public TelegramMessageSender(TelegramBotService telegramBotService) {
        this.telegramBotService = telegramBotService;
    }
    /**
     * Отправляет сообщение через Telegram Bot API.
     *
     * Метод использует {@link TelegramBotService} для выполнения запроса на отправку сообщения в Telegram.
     *
     * @param message Объект {@link SendMessage}, содержащий текст сообщения и настройки отправки.
     * @throws TelegramApiException Если произошла ошибка при отправке сообщения через Telegram Bot API.
     */
    @Override
    public void sendMessage(SendMessage message) throws TelegramApiException {
        telegramBotService.execute(message);
    }

    /**
     * Редактирует текст ранее отправленного сообщения через Telegram Bot API.
     *
     * Метод использует {@link TelegramBotService} для выполнения запроса на редактирование сообщения.
     *
     * @param message Объект {@link EditMessageText}, содержащий новые данные для редактирования сообщения.
     * @throws TelegramApiException Если произошла ошибка при редактировании сообщения через Telegram Bot API.
     */
    @Override
    public void editMessage(EditMessageText message) throws TelegramApiException {
        telegramBotService.execute(message);
    }
}
