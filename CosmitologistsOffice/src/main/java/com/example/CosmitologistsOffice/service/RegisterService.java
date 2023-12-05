package com.example.CosmitologistsOffice.service;

import org.telegram.telegrambots.meta.api.objects.Message;
public interface RegisterService {
    void registerUser(Message msg);
    void register(long chatId);
    /*ChatUser getChatUser (long chatId);*/
}
