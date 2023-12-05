package com.example.CosmitologistsOffice.service.impl;

import com.example.CosmitologistsOffice.constants.StaticConstant;
import com.example.CosmitologistsOffice.model.ChatUser;
import com.example.CosmitologistsOffice.repository.UserRepository;
import com.example.CosmitologistsOffice.service.RegisterService;
import com.example.CosmitologistsOffice.service.SendMessageForUser;
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

@Slf4j
@Service
public class RegisterServiceImpl implements RegisterService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StaticConstant staticConstant;
    @Autowired
    private SendMessageForUser sendMessageForUser;

    @Override
    public void registerUser(Message msg) {
        if (!userRepository.findByUsername(msg.getChat().getUserName())) {
            var chatId = msg.getChatId();
            var chat = msg.getChat();
            ChatUser chatUser = new ChatUser();
            chatUser.setChatId(chatId);
            chatUser.setUsername(chat.getUserName());
            chatUser.setFirstName(chat.getFirstName());
            chatUser.setLastName(chat.getLastName());
            chatUser.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
            userRepository.save(chatUser);
            log.info("user saved: " + chatUser);
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

        sendMessageForUser.executeMessage(message);
    }

    private List<List<InlineKeyboardButton>> getListsButtonsForRegister() {
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        var yesButton = new InlineKeyboardButton();
        yesButton.setText("Yes");
        yesButton.setCallbackData(staticConstant.YES_BUTTON);
        var noButton = new InlineKeyboardButton();
        noButton.setText("No");
        noButton.setCallbackData(staticConstant.NO_BUTTON);
        rowInLine.add(yesButton);
        rowInLine.add(noButton);
        rowsInLine.add(rowInLine);
        return rowsInLine;
    }
}
