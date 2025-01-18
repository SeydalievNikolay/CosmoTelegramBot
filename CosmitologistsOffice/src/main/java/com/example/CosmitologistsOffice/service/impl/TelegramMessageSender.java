package com.example.CosmitologistsOffice.service.impl;

import com.example.CosmitologistsOffice.service.MessageSender;
import com.example.CosmitologistsOffice.service.TelegramBotService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class TelegramMessageSender implements MessageSender {
    private final TelegramBotService telegramBotService;

    public TelegramMessageSender(TelegramBotService telegramBotService) {
        this.telegramBotService = telegramBotService;
    }

    @Override
    public void sendMessage(SendMessage message) throws TelegramApiException {
        telegramBotService.execute(message);
    }

    @Override
    public void editMessage(EditMessageText message) throws TelegramApiException {
        telegramBotService.execute(message);
    }
}
