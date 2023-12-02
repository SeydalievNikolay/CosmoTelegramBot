package com.example.CosmitologistsOffice.service;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public interface SendMessageForUser {
    void executeEditMessageText(String text, long chatId, long messageId);
    void executeMessage(SendMessage message);
    void prepareAndSendMessage(long chatId, String textToSend);
}
