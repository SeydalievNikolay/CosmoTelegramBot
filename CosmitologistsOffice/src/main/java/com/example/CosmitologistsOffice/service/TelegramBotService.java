package com.example.CosmitologistsOffice.service;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public interface TelegramBotService {

    void handleCallbackQuery(CallbackQuery callbackQuery);

    void execute(EditMessageText message) throws TelegramApiException;

    void execute(SendMessage message) throws TelegramApiException;

}
