package com.example.CosmitologistsOffice.service.impl;

import com.example.CosmitologistsOffice.model.ChatUser;
import com.example.CosmitologistsOffice.repository.ChatUserRepository;
import com.example.CosmitologistsOffice.service.ChatUserService;
import lombok.extern.slf4j.Slf4j;

import java.sql.Timestamp;
import java.util.Optional;

/**
 * Сервис для работы с пользователями чата. Этот сервис управляет данными пользователей, такими как chatId, имя, номер телефона и т.д.
 * Сервис позволяет создавать, обновлять и получать информацию о пользователях чата.
 */
@Slf4j
public class ChatUserServiceImpl implements ChatUserService {
    private final ChatUserRepository chatUserRepository;

    public ChatUserServiceImpl(ChatUserRepository chatUserRepository) {
        this.chatUserRepository = chatUserRepository;
    }

    /**
     * Проверяет, существует ли пользователь с данным chatId. Если пользователь существует, обновляет его данные.
     * Если пользователя с таким chatId нет, создается новый пользователь.
     *
     * @param chatId    Идентификатор чата пользователя.
     * @param firstName Имя пользователя.
     * @param lastName  Фамилия пользователя.
     * @param username  Юзернейм пользователя.
     * @param phoneNumber Номер телефона пользователя.
     */
    @Override
    public void ensureUserExists(Long chatId, String firstName, String lastName, String username, String phoneNumber) {
        Optional<ChatUser> existingUser = chatUserRepository.findByChatId(chatId);

        if (existingUser.isPresent()) {
            ChatUser user = existingUser.get();
            user.setChatId(chatId);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setUsername(username);
            user.setPhoneNumber(phoneNumber);
            chatUserRepository.save(user);
        } else {
            ChatUser newUser = new ChatUser();
            newUser.setChatId(chatId);
            newUser.setFirstName(firstName);
            newUser.setLastName(lastName);
            newUser.setUsername(username);
            newUser.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
            chatUserRepository.save(newUser);
        }
    }

    /**
     * Проверяет наличие номера телефона для пользователя с данным chatId.
     * Если номер телефона существует, он сохраняется. Если пользователя нет, он создается с указанным номером.
     *
     * @param chatId     Идентификатор чата пользователя.
     * @param phoneNumber Номер телефона, который нужно сохранить.
     */
    @Override
    public void existPhoneNumber(long chatId, String phoneNumber) {
        log.info("Проверка существования номера телефона в БД");
         Optional<ChatUser> existingUser = chatUserRepository.findByChatId(chatId);

        if (existingUser.isPresent()) {
            ChatUser user = existingUser.get();
            user.setPhoneNumber(phoneNumber);
            chatUserRepository.save(user);
            log.info("Такой номер уже существует в БД");
        }else {
            log.info("Создаем пользователя, если нет в БД и присваиваем ему номер телефона");
            ChatUser newUser = new ChatUser();
            newUser.setId(System.currentTimeMillis());
            newUser.setChatId(chatId);
            newUser.setFirstName("");
            newUser.setLastName("");
            newUser.setUsername("");
            newUser.setPhoneNumber(phoneNumber);
            chatUserRepository.save(newUser);
        }
    }

    /**
     * Получает номер телефона пользователя по его chatId.
     * Если пользователь не найден, возвращает null.
     *
     * @param chatId Идентификатор чата пользователя.
     * @return Номер телефона пользователя или null, если пользователь не найден.
     */
    @Override
    public String getPhoneNumberByChatId(long chatId) {
        String phone = null;

        Optional<ChatUser> userOptional = chatUserRepository.findByChatId(chatId);

        if (userOptional.isPresent()) {
            ChatUser user = userOptional.get();
            phone = user.getPhoneNumber();
        } else {
            log.error("Такого chatId нет в БД: {}", chatId);
        }
        return phone;
    }

    /**
     * Получает пользователя по его chatId.
     * Возвращает объект {@link ChatUser}, если пользователь найден, иначе возвращает пустой {@link Optional}.
     *
     * @param chatId Идентификатор чата пользователя.
     * @return Объект {@link Optional<ChatUser>}, содержащий пользователя, если он найден.
     */
    @Override
    public Optional<ChatUser> getUserByChatId(Long chatId) {
        return chatUserRepository.findByChatId(chatId);
    }
}
