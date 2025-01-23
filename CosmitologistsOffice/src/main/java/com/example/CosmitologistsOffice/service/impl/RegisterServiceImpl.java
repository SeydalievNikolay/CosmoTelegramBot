package com.example.CosmitologistsOffice.service.impl;

import com.example.CosmitologistsOffice.constants.StaticConstant;
import com.example.CosmitologistsOffice.service.MessageSender;
import com.example.CosmitologistsOffice.service.RegisterService;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

/**
 * Этот сервис управляет процессом регистрации пользователя, включая отправку сообщений и кнопок для выбора дальнейших действий.
 */
@Slf4j
public class RegisterServiceImpl implements RegisterService {

    private final MessageSender telegramMessageSender;

    public RegisterServiceImpl(MessageSender telegramMessageSender) {
        this.telegramMessageSender = telegramMessageSender;
    }

    /**
     * Запускает процесс регистрации пользователя, отправляя сообщение с просьбой предоставить номер телефона.
     * В сообщении также присутствует кнопка для отмены регистрации.
     *
     * @param chatId Идентификатор чата пользователя в Telegram.
     */
    @Override
    public void registerUser(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));

        message.setText("Пожалуйста, отправьте свой номер телефона в формате 8XXXXXXXXX");

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = getListsButtonsForNextStep();
        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);
        try {
            telegramMessageSender.sendMessage(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Отправляет сообщение пользователю с предложением оставить номер телефона.
     * Сообщение содержит кнопки для ответа "Да" или "Нет".
     *
     * @param chatId Идентификатор чата пользователя в Telegram.
     * @throws TelegramApiException Если возникает ошибка при отправке сообщения.
     */
    @Override
    public void register(long chatId) throws TelegramApiException {

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Хотите оставить свой номер для связи?");

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = getListsButtonsForRegister();
        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);

        telegramMessageSender.sendMessage(message);
    }

    /**
     * Отправляет сообщение пользователю с уведомлением о том, что регистрация отменена.
     * В сообщении присутствует кнопка для начала регистрации заново.
     *
     * @param chatId Идентификатор чата пользователя в Telegram.
     */
    @Override
    public void sendCancelMessage(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Регистрация отменена. Для продолжения нажмите на /start в нижнем меню");

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();

        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText("Начать заново");
        button.setCallbackData("/start");

        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(button);

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(row);

        markupInLine.setKeyboard(keyboard);
        message.setReplyMarkup(markupInLine);

        try {
            telegramMessageSender.sendMessage(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Формирует клавиатуру для отображения кнопок "Да" и "Нет" для решения пользователя оставить номер телефона.
     *
     * @return Список строк клавиатуры с кнопками "Да" и "Нет".
     */
    private List<List<InlineKeyboardButton>> getListsButtonsForRegister() {
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        var yesButton = new InlineKeyboardButton();
        yesButton.setText("Да");
        yesButton.setCallbackData(StaticConstant.YES_BUTTON_SEND_PHONE_NUMBER);
        var noButton = new InlineKeyboardButton();
        noButton.setText("Нет");
        noButton.setCallbackData(StaticConstant.NO_BUTTON_SEND_PHONE_NUMBER);
        rowInLine.add(yesButton);
        rowInLine.add(noButton);
        rowsInLine.add(rowInLine);
        return rowsInLine;
    }

    /**
     * Формирует клавиатуру для следующего шага в процессе регистрации, включая кнопку отмены.
     *
     * @return Список строк клавиатуры с кнопкой отмены регистрации.
     */
    private List<List<InlineKeyboardButton>> getListsButtonsForNextStep() {
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        var cancelRegistrationButton = new InlineKeyboardButton();
        cancelRegistrationButton.setText("Отмена");
        cancelRegistrationButton.setCallbackData(StaticConstant.CANCEL_REGISTRATION);
        rowInLine.add(cancelRegistrationButton);

        rowsInLine.add(rowInLine);
        return rowsInLine;
    }

    /**
     * Отправляет номер телефона пользователю и информирует его о успешной регистрации.
     *
     * @param chatId Идентификатор чата пользователя в Telegram.
     * @param phoneNumber Номер телефона, который был зарегистрирован.
     */
    @Override
    public void sendPhoneNumberToCosmetologist(long chatId, String phoneNumber) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);

         message.setText("Ваш номер телефон успешно зарегистрирован. Мы свяжемся с вами. Спасибо");

        try {
            telegramMessageSender.sendMessage(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
