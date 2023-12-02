package com.example.CosmitologistsOffice.service.impl;

import com.example.CosmitologistsOffice.constants.StaticConstant;
import com.example.CosmitologistsOffice.service.SendMessageForUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
@Service
@Slf4j
public class SendMessageForUserImpl implements SendMessageForUser {
    @Autowired
    private StaticConstant staticConstant;
    @Autowired
    private AbsSender absSender;
    public void executeEditMessageText(String text, long chatId, long messageId){
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setMessageId((int) messageId);

        try {
            absSender.execute(message);
        } catch (TelegramApiException e) {
            log.error(staticConstant.ERROR_TEXT + e.getMessage());
        }
    }

    public void executeMessage(SendMessage message){
        try {
            absSender.execute(message);
        } catch (TelegramApiException e) {
            log.error(staticConstant.ERROR_TEXT + e.getMessage());
        }
    }

    public void prepareAndSendMessage(long chatId, String textToSend){
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        executeMessage(message);
    }
}
