package com.example.CosmitologistsOffice.service;

import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public interface RegisterService {

    void registerUser(long chatId);

    void register(long chatId) throws TelegramApiException;

    void sendCancelMessage(long chatId);
    void sendPhoneNumberToCosmetologist(long chatId, String phoneNumber);
}
