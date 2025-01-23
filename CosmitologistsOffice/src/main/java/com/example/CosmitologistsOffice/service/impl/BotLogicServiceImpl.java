package com.example.CosmitologistsOffice.service.impl;

import com.example.CosmitologistsOffice.model.ChatUser;
import com.example.CosmitologistsOffice.service.BotLogicService;
import com.example.CosmitologistsOffice.service.ChatUserService;
import com.example.CosmitologistsOffice.service.MessageSender;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Реализация логики работы бота для взаимодействия с пользователями.
 * Этот сервис обрабатывает команды бота, такие как /start, /help, отправку сообщений пользователям и формирование клавиатуры.
 */
@Slf4j
public class BotLogicServiceImpl implements BotLogicService {
    private final ChatUserService chatUserService;
    private final MessageSender messageSender;

    public BotLogicServiceImpl(ChatUserService chatUserService,
                               MessageSender messageSender) {
        this.chatUserService = chatUserService;
        this.messageSender = messageSender;

    }

    /**
     * Обработка команды /start, которая отправляется пользователю при первом взаимодействии с ботом.
     * Проверяется, существует ли пользователь с данным chatId. Если пользователя нет в базе данных, он создается.
     *
     * @param chatId     Идентификатор чата пользователя.
     * @param firstName  Имя пользователя.
     * @param lastName   Фамилия пользователя.
     * @param username   Юзернейм пользователя.
     * @param phoneNumber Номер телефона пользователя.
     */
    @Override
    public void startCommandReceived(Long chatId, String firstName, String lastName, String username, String phoneNumber) {
        log.info("Получена команда /start для чата с ID: {}", chatId);
        log.info("Имя пользователя: {}", firstName);

        Optional<ChatUser> optionalChatUser = chatUserService.getUserByChatId(chatId);

        if (optionalChatUser.isEmpty()) {
            log.info("Пользователь с chatId {} не найден, создаем нового.", chatId);
            chatUserService.ensureUserExists(chatId, firstName, lastName, username, phoneNumber);
        } else {
            ChatUser existingUser = optionalChatUser.get();
            if (!existingUser.getFirstName().equals(firstName) ||
                    !existingUser.getUsername().equals(username)) {
                log.info("Обновляем данные пользователя с chatId: {}", chatId);
                chatUserService.ensureUserExists(chatId, firstName, lastName, username,phoneNumber);
            } else {
                log.info("Пользователь с chatId {} уже существует и его данные не изменились.", chatId);
            }
        }

        String answer = "Здравствуйте, " + firstName +
                "! Рада видеть Вас!" + EmojiParser.parseToUnicode(":blush:") +
                "\n\nДля начала работы воспользуйтесь кнопками ниже";

        log.info("Формирование ответа пользователю: {}", firstName);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(answer);
        message.setReplyMarkup(getInlineKeyboardMarkup());

        log.info("Отправка сообщения пользователю: {}", firstName);
        try {
            messageSender.sendMessage(message);
            log.info("Сообщение успешно отправлено");
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения", e);
        }
    }

    /**
     * Обработка команды /help, которая предоставляет пользователю информацию о функциональности бота.
     *
     * @param chatId Идентификатор чата пользователя.
     * @param message Сообщение, переданное пользователем.
     */
    @Override
    public void sendHelpMessage(long chatId, String message) {
        log.info("Получена команда /help пользователем: {}", message);
        String helpText = "Привет! Я бот для записи на услуги косметолога.\n\n" +
                "Вот несколько команд, которые ты можешь использовать:\n\n" +
                " **Выбрать услугу** - выбери услугу, которую хочешь записать.\n" +
                " **Помощь** - получи информацию о том, как использовать бота.\n" +
                " **Отменить или перенести запись** - отмени или перенеси свою запись.\n\n" +
                "Как только ты выберешь услугу и дату, я помогу тебе с записью!";

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(helpText);
        sendMessage.setReplyMarkup(getInlineKeyboardMarkup());

        try {
            messageSender.sendMessage(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения помощи", e);
        }
    }

    /**
     * Отправка сообщения об ошибке пользователю.
     *
     * @param chatId       Идентификатор чата пользователя.
     * @param errorMessage Сообщение об ошибке.
     */
    @Override
    public void sendErrorMessage(long chatId, String errorMessage) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(errorMessage);

        try {
            messageSender.sendMessage(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения об ошибке", e);
        }
    }

    /**
     * Отправка успешного сообщения пользователю.
     *
     * @param chatId Идентификатор чата пользователя.
     * @param message Сообщение, которое нужно отправить.
     */
    @Override
    public void sendSuccessMessage(Long chatId, String message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText(message);

        try {
            messageSender.sendMessage(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения о подтверждении", e);
        }
    }

    /**
     * Формирует клавиатуру с кнопками для пользователя.
     * Включает кнопки для выбора услуги, отмены записи и вызова справки.
     *
     * @return Объект клавиатуры с кнопками.
     */
    private InlineKeyboardMarkup getInlineKeyboardMarkup() {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboardRows = new ArrayList<>();

        List<InlineKeyboardButton> selectServiceRow = new ArrayList<>();
        InlineKeyboardButton selectServiceButton = new InlineKeyboardButton();
        selectServiceButton.setText("Выбрать услугу");
        selectServiceButton.setCallbackData("select_service");
        selectServiceRow.add(selectServiceButton);
        keyboardRows.add(selectServiceRow);

        List<InlineKeyboardButton> cancelRescheduleRow = new ArrayList<>();
        InlineKeyboardButton cancelRescheduleButton = new InlineKeyboardButton();
        cancelRescheduleButton.setText("Отменить или перенести запись");
        cancelRescheduleButton.setCallbackData("cancel_or_reschedule_recording_");
        cancelRescheduleRow.add(cancelRescheduleButton);
        keyboardRows.add(cancelRescheduleRow);

        List<InlineKeyboardButton> helpRow = new ArrayList<>();
        InlineKeyboardButton helpButton = new InlineKeyboardButton();
        helpButton.setText("Справка");
        helpButton.setCallbackData("show_help");
        helpRow.add(helpButton);
        keyboardRows.add(helpRow);

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }
}
