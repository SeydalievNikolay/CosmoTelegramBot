package com.example.CosmitologistsOffice.service;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

public interface TelegramBotService {

    void handleCallbackQuery(CallbackQuery callbackQuery);
}
