package com.example.CosmitologistsOffice.service;

import com.example.CosmitologistsOffice.model.ChatUser;

import java.util.Optional;

public interface ChatUserService {
    void ensureUserExists(Long chatId, String firstName, String lastName, String username, String phoneNumber);
    void existPhoneNumber(long chatId, String phoneNumber);
    String getPhoneNumberByChatId(long chatId);
    Optional<ChatUser> getUserByChatId(Long chatId);
}
