package com.example.CosmitologistsOffice.service.impl;

import com.example.CosmitologistsOffice.model.ChatUser;
import com.example.CosmitologistsOffice.repository.ChatUserRepository;
import com.example.CosmitologistsOffice.service.ChatUserService;
import lombok.extern.slf4j.Slf4j;

import java.sql.Timestamp;
import java.util.Optional;

@Slf4j
public class ChatUserServiceImpl implements ChatUserService {
    private final ChatUserRepository chatUserRepository;

    public ChatUserServiceImpl(ChatUserRepository chatUserRepository) {
        this.chatUserRepository = chatUserRepository;
    }

    @Override
    public void ensureUserExists(Long chatId, String firstName, String lastName, String username) {
        // Проверяем, существует ли уже пользователь с таким chatId
        Optional<ChatUser> existingUser = chatUserRepository.findByChatId(chatId);

        if (existingUser.isPresent()) {
            // Если пользователь существует, обновляем его данные
            ChatUser user = existingUser.get();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setUsername(username);
            chatUserRepository.save(user);  // Сохраняем обновленные данные
        } else {
            // Если пользователя нет, создаем нового
            ChatUser newUser = new ChatUser();
            newUser.setChatId(chatId);
            newUser.setFirstName(firstName);
            newUser.setLastName(lastName);
            newUser.setUsername(username);
            newUser.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
            chatUserRepository.save(newUser);
        }
    }

    public Optional<ChatUser> getUserByChatId(Long chatId) {
        return chatUserRepository.findByChatId(chatId);
    }
}
