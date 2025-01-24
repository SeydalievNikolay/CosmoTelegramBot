package com.example.CosmitologistsOffice.config;

import com.example.CosmitologistsOffice.service.impl.TelegramBotServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
public class BotInitializer {
    @Autowired
    public BotInitializer(TelegramBotsApi telegramBotsApi, TelegramBotServiceImpl telegramBotService) {
        try {
            telegramBotsApi.registerBot(telegramBotService);
        } catch (TelegramApiException e) {
            log.error("Произошла ошибка при регистрации бота:" + e.getMessage());
        }
    }
}
