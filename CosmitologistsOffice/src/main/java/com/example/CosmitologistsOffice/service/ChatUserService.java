package com.example.CosmitologistsOffice.service;

import com.example.CosmitologistsOffice.model.ChatUser;

import java.util.Optional;

public interface ChatUserService {
    void ensureUserExists(Long chatId, String firstName, String lastName, String username);
    Optional<ChatUser> getUserByChatId(Long chatId);
}
