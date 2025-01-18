package com.example.CosmitologistsOffice.service;

import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public interface RegisterService {
    void registerUser(Message msg);

    void register(long chatId) throws TelegramApiException;
}
