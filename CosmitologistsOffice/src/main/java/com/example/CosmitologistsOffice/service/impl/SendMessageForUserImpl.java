package com.example.CosmitologistsOffice.service.impl;

import com.example.CosmitologistsOffice.constants.StaticConstant;
import com.example.CosmitologistsOffice.service.SendMessageForUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
@Slf4j
public class SendMessageForUserImpl implements SendMessageForUser {
    private StaticConstant staticConstant;
    private TelegramLongPollingBot execute;

    @Override
    public BotApiMethod<?> executeEditMessageText(String text, long chatId, long messageId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setMessageId((int) messageId);

        return message;
    }

    @Override
    public void executeMessage(SendMessage message) {
        try {
            execute.execute(message);
        } catch (TelegramApiException e) {
            log.error(staticConstant.ERROR_TEXT + e.getMessage());
        }
    }

    @Override
    public void prepareAndSendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        executeMessage(message);
    }
}
