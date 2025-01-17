package com.example.CosmitologistsOffice.service.impl;

import com.example.CosmitologistsOffice.service.SendMessageForUserService;
import com.vdurmont.emoji.EmojiParser;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Slf4j
@Service
public class SendMessageForUserServiceImpl implements SendMessageForUserService {

    private TelegramBotServiceImpl telegramBotService;

    @Autowired
    public void setTelegramBotService(@Lazy TelegramBotServiceImpl telegramBotService) {
        this.telegramBotService = telegramBotService;
    }

    @Override
    public void executeEditMessageText(String text, long chatId, long messageId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setMessageId((int) messageId);

        try {
            telegramBotService.execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при редактировании сообщения", e);
        }
    }

    @Override
    public void executeMessage(SendMessage message) {
        try {
            telegramBotService.execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения", e);
        }
    }

    @Override
    public void prepareAndSendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        executeMessage(message);
    }

    @Override
    public void startCommandReceived(long chatId, String name) {
        log.info("Получена команда /start для чата с ID: {}", chatId);
        log.info("Имя пользователя: {}", name);

        String answer = "Здравствуйте, " + name + "! Рады видеть Вас!" + EmojiParser.parseToUnicode(":blush:") +
                "\n\nДля начала работы воспользуйтесь кнопками ниже или введите команду:" +
                "\n- Выбрать услугу" +
                "\n- Выбрать дату и время" +
                "\n- Отменить или перенести запись" +
                "\n- Справка";

        log.info("Формирование ответа пользователю" + name);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(answer);
        message.setReplyMarkup(getInlineKeyboardMarkup());
        log.info("Отправка сообщения пользователю");
        try {
            telegramBotService.execute(message);
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
            telegramBotService.execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения", e);
        }
    }

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
            telegramBotService.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения помощи", e);
        }
    }

    public void sendErrorMessage(long chatId, String errorMessage) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(errorMessage);

        try {
            telegramBotService.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения об ошибке", e);
        }
    }

/*    private ReplyKeyboardMarkup getKeyboardMarkup() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("Выбрать услугу");
        row.add("Выбрать дату и время");
        row.add("Отменить или перенести запись");
        row.add("Справка");
        keyboardRows.add(row);
        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }*/

    private InlineKeyboardMarkup getInlineKeyboardMarkup() {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboardRows = new ArrayList<>();
        List<InlineKeyboardButton> listRows = new ArrayList<>();

        InlineKeyboardButton selectServiceButton = new InlineKeyboardButton();
        selectServiceButton.setText("Выбрать услугу");
        selectServiceButton.setCallbackData("select_service");
        listRows.add(selectServiceButton);

        InlineKeyboardButton selectDateTimeButton = new InlineKeyboardButton();
        selectDateTimeButton.setText("Выбрать дату и время");
        selectDateTimeButton.setCallbackData("select_date_time");
        listRows.add(selectDateTimeButton);

        InlineKeyboardButton cancelRescheduleButton = new InlineKeyboardButton();
        cancelRescheduleButton.setText("Отменить или перенести запись");
        cancelRescheduleButton.setCallbackData("cancel_reschedule");
        listRows.add(cancelRescheduleButton);

        InlineKeyboardButton helpButton = new InlineKeyboardButton();
        helpButton.setText("Справка");
        helpButton.setCallbackData("show_help");
        listRows.add(helpButton);

        keyboardRows.add(listRows);
        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }


    public void sendSuccessMessage(Long chatId, String message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText(message);

        try {
            telegramBotService.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения о подтверждении", e);
        }
    }

}
