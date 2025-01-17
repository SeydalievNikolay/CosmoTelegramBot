package com.example.CosmitologistsOffice.service.impl;

import com.example.CosmitologistsOffice.constants.StaticConstant;
import com.example.CosmitologistsOffice.exceptions.ChatUserNotFoundException;
import com.example.CosmitologistsOffice.model.Appointment;
import com.example.CosmitologistsOffice.model.ChatUser;
import com.example.CosmitologistsOffice.model.Cosmetologist;
import com.example.CosmitologistsOffice.repository.CosmetologistRepository;
import com.example.CosmitologistsOffice.repository.ChatUserRepository;
import com.example.CosmitologistsOffice.service.AppointmentService;
import com.example.CosmitologistsOffice.service.RegisterService;
import com.example.CosmitologistsOffice.service.SendMessageForUserService;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
@NoArgsConstructor
public class RegisterServiceImpl implements RegisterService {
    @Autowired
    private ChatUserRepository chatUserRepository;
    @Autowired
    private SendMessageForUserService sendMessageForUserService;

    @Autowired
    private NotificationServiceImpl notificationService;

    @Autowired
    private CosmetologistRepository cosmetologistRepository;

    @Autowired
    private AppointmentService appointmentService;

    @Override
    public void registerUser(Message msg) {
        Optional<ChatUser> existingUser = chatUserRepository.findById(msg.getChatId());
        if (existingUser.isEmpty()) {
            ChatUser chatUser = new ChatUser();
            chatUser.setChatId(msg.getChatId());
            chatUser.setUsername(msg.getChat().getUserName());
            chatUser.setFirstName(msg.getChat().getFirstName());
            chatUser.setLastName(msg.getChat().getLastName());
            chatUser.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            registerCosmetologist(chatUser, msg.getChatId());

            chatUserRepository.save(chatUser);
            log.info("User saved: " + chatUser);
        } else {
            log.info("User already exists: " + existingUser.get());
        }
    }


    @Override
    public void register(long chatId) {

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Do you really want to register?");

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = getListsButtonsForRegister();
        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);

        sendMessageForUserService.executeMessage(message);
    }

    private List<List<InlineKeyboardButton>> getListsButtonsForRegister() {
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        var yesButton = new InlineKeyboardButton();
        yesButton.setText("Да");
        yesButton.setCallbackData(StaticConstant.YES_BUTTON);
        var noButton = new InlineKeyboardButton();
        noButton.setText("Нет");
        noButton.setCallbackData(StaticConstant.NO_BUTTON);
        rowInLine.add(yesButton);
        rowInLine.add(noButton);
        rowsInLine.add(rowInLine);
        return rowsInLine;
    }

    public void registerCosmetologist(ChatUser chatUser, Long chatId) {
        Cosmetologist cosmetologist = cosmetologistRepository.findByTelegramChatId(chatId);

        if (cosmetologist == null) {
            cosmetologist = new Cosmetologist();
            cosmetologist.setName("Александра");
            cosmetologist.setTelegramChatId(341641617L); // Это значение можно параметризовать или брать из другого источника
            cosmetologist.setNickName("Aleksandra_Seydalieva");
            cosmetologistRepository.save(cosmetologist);
        }

        // Создаем новую запись
        Appointment newAppointment = appointmentService.createNewAppointment(cosmetologist, chatUser);

        // Уведомляем косметолога
        notificationService.notifyCosmetologist(newAppointment);
    }

    public void registerCosmetologistFromUpdate(Message msg) {
        // Извлечение данных из объекта Message
        String firstName = msg.getFrom().getFirstName();  // Имя
        String lastName = msg.getFrom().getLastName();    // Фамилия
        String userName = msg.getFrom().getUserName();    // Юзернейм
        Long telegramChatId = msg.getFrom().getId();      // ID чата (используется как уникальный идентификатор)

        // Пытаемся найти косметолога по telegramChatId
        Cosmetologist cosmetologist = cosmetologistRepository.findByTelegramChatId(telegramChatId);

        // Если косметолог не найден, создаем нового
        if (cosmetologist == null) {
            cosmetologist = new Cosmetologist();
            cosmetologist.setName(firstName + " " + lastName);  // Формируем имя
            cosmetologist.setTelegramChatId(telegramChatId);   // ID Telegram чата
            cosmetologist.setNickName(userName);                // Никнейм

            cosmetologistRepository.save(cosmetologist); // Сохраняем косметолога в базе данных
            log.info("Cosmetologist registered: " + cosmetologist);
        } else {
            log.info("Cosmetologist already exists: " + cosmetologist);
        }

        // Дополнительно можно зарегистрировать или обновить назначение на прием для косметолога
        // Например, привязать его к записи или выполнить другие действия
    }
    public void registerAppointmentForCosmetologist(Message msg) {
        // Получаем данные из обновления
        Long chatId = msg.getChatId();  // Получаем chatId
        Cosmetologist cosmetologist = findOrCreateCosmetologist(msg);  // Найдем или создадим косметолога

        // Получаем объект ChatUser для записи
        ChatUser chatUser = chatUserRepository.findByChatId(chatId)
                .orElseThrow(() -> new ChatUserNotFoundException("ChatUser not found for chatId: " + chatId));

        // Создаем новую запись для приема
        Appointment newAppointment = appointmentService.createNewAppointment(cosmetologist, chatUser);

        // Уведомляем косметолога о новом приеме
        notificationService.notifyCosmetologist(newAppointment);
    }

    public Cosmetologist findOrCreateCosmetologist(Message msg) {
        // Извлекаем данные из сообщения
        String firstName = msg.getFrom().getFirstName();
        String lastName = msg.getFrom().getLastName();
        String userName = msg.getFrom().getUserName();
        Long telegramChatId = msg.getFrom().getId();

        // Пытаемся найти косметолога по telegramChatId
        Cosmetologist cosmetologist = cosmetologistRepository.findByTelegramChatId(telegramChatId);

        if (cosmetologist == null) {
            cosmetologist = new Cosmetologist();
            cosmetologist.setName(firstName + " " + lastName);
            cosmetologist.setTelegramChatId(telegramChatId);
            cosmetologist.setNickName(userName);

            cosmetologistRepository.save(cosmetologist);  // Сохраняем в базе
        }

        return cosmetologist;
    }

}
