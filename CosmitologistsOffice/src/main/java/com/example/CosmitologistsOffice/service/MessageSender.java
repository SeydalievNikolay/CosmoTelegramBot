package com.example.CosmitologistsOffice.service;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public interface MessageSender {
    void sendMessage(SendMessage message) throws TelegramApiException;

    void editMessage(EditMessageText message) throws TelegramApiException;
}
