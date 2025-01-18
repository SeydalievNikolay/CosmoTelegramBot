package com.example.CosmitologistsOffice.service;

public interface BotLogicService {
    void startCommandReceived(Long chatId, String firstName, String lastName, String username);

    void sendMessage(long chatId, String textToSend);

    void sendHelpMessage(long chatId, String message);

    void sendErrorMessage(long chatId, String errorMessage);

    void sendSuccessMessage(Long chatId, String message);
}
