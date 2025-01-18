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

@Slf4j
public class BotLogicServiceImpl implements BotLogicService {
    private final ChatUserService chatUserService;
    private final MessageSender messageSender;
    private final TelegramMessageSender telegramMessageSender;

    public BotLogicServiceImpl(ChatUserService chatUserService,
                               MessageSender messageSender,
                               TelegramMessageSender telegramMessageSender) {
        this.chatUserService = chatUserService;
        this.messageSender = messageSender;
        this.telegramMessageSender = telegramMessageSender;
    }

    @Override
    public void startCommandReceived(Long chatId, String firstName, String lastName, String username) {
        log.info("Получена команда /start для чата с ID: {}", chatId);
        log.info("Имя пользователя: {}", firstName);

        // Проверка и создание/обновление пользователя
        chatUserService.ensureUserExists(chatId, firstName, lastName, username);

        // Получение пользователя после его создания/обновления
        Optional<ChatUser> optionalChatUser  = chatUserService.getUserByChatId(chatId);

        if (optionalChatUser.isEmpty()) {
            log.error("Ошибка: Пользователь с chatId = {} не найден.", chatId);
            return;  // если по каким-то причинам пользователя не нашли
        }

        ChatUser chatUser = optionalChatUser.get();

        String answer = "Здравствуйте, " + chatUser.getFirstName() + "! Рады видеть Вас!" + EmojiParser.parseToUnicode(":blush:") +
                "\n\nДля начала работы воспользуйтесь кнопками ниже или введите команду:" +
                "\n- выбрать услугу" +
                "\n- выбрать дату и время" +
                "\n- отменить или перенести запись" +
                "\n- справка";

        log.info("Формирование ответа пользователю" + chatUser.getFirstName());

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(answer);
        message.setReplyMarkup(getInlineKeyboardMarkup());
        log.info("Отправка сообщения пользователю" + chatUser.getFirstName());
        try {
            messageSender.sendMessage(message);
            log.info("Сообщение успешно отправлено");
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения", e);
        }
    }

    @Override
    public void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        getInlineKeyboardMarkup();

        try {
            messageSender.sendMessage(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения", e);
        }
    }

    @Override
    public void sendHelpMessage(long chatId, String message) {
        log.info("Received /help command from user: {}", message);
        String helpText = "Привет! Я бот для записи на услуги косметолога.\n\n" +
                "Вот несколько команд, которые ты можешь использовать:\n\n" +
                "1. **Выбрать услугу** - выбери услугу, которую хочешь записать.\n" +
                "2. **Выбрать дату и время** - выбери дату и время для записи.\n" +
                "3. **Помощь** - получи информацию о том, как использовать бота.\n" +
                "4. **Отменить или перенести запись** - отмени или перенеси свою запись.\n\n" +
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
    private InlineKeyboardMarkup getInlineKeyboardMarkup() {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboardRows = new ArrayList<>();

        // Строка с выбором услуги
        List<InlineKeyboardButton> selectServiceRow = new ArrayList<>();
        InlineKeyboardButton selectServiceButton = new InlineKeyboardButton();
        selectServiceButton.setText("Выбрать услугу");
        selectServiceButton.setCallbackData("service_");
        selectServiceRow.add(selectServiceButton);
        keyboardRows.add(selectServiceRow);

        // Строка выбора даты и времени
        List<InlineKeyboardButton> selectDateTimeRow = new ArrayList<>();
        InlineKeyboardButton selectDateTimeButton = new InlineKeyboardButton();
        selectDateTimeButton.setText("Выбрать дату и время");
        selectDateTimeButton.setCallbackData("select_date_time");
        selectDateTimeRow.add(selectDateTimeButton);
        keyboardRows.add(selectDateTimeRow);

        // Строка отмены или переноса записи
        List<InlineKeyboardButton> cancelRescheduleRow = new ArrayList<>();
        InlineKeyboardButton cancelRescheduleButton = new InlineKeyboardButton();
        cancelRescheduleButton.setText("Отменить или перенести запись");
        cancelRescheduleButton.setCallbackData("cancel_reschedule");
        cancelRescheduleRow.add(cancelRescheduleButton);
        keyboardRows.add(cancelRescheduleRow);

        // Строка справки
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
