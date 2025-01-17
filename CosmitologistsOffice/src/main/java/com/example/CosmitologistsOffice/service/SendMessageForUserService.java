package com.example.CosmitologistsOffice.service;

import com.example.CosmitologistsOffice.service.impl.TelegramBotServiceImpl;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public interface SendMessageForUserService {
    void setTelegramBotService(TelegramBotServiceImpl telegramBotService);
    void executeEditMessageText(String text, long chatId, long messageId);
    void executeMessage(SendMessage message);
    void prepareAndSendMessage(long chatId, String textToSend);
    void startCommandReceived(long chatId, String name);
    void sendMessage(long chatId, String textToSend);
    void sendHelpMessage(long chatId, String message);
    void sendErrorMessage(long chatId, String errorMessage);
    void sendSuccessMessage(Long chatId, String message);
}
