package com.example.CosmitologistsOffice.service;

import com.example.CosmitologistsOffice.config.BotConfig;
import com.example.CosmitologistsOffice.constants.StaticConstant;
import com.example.CosmitologistsOffice.model.ChatUser;
import com.example.CosmitologistsOffice.repository.UserRepository;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class TelegramBotService extends TelegramLongPollingBot {
    @Autowired
    private RegisterService register;
    @Autowired
    private UserRepository userRepository;
    private  StaticConstant staticConstant;
    @Autowired
    private SendMessageForUser sendMessageForUser;
    final BotConfig config;

    public TelegramBotService(BotConfig botConfig) {
        this.config = botConfig;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "приветствие"));
        listOfCommands.add(new BotCommand("/register", "регистрация"));
        listOfCommands.add(new BotCommand("/help", "помощь"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bots command list" + e.getMessage());
        }

    }


    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            if(messageText.contains("/send") && config.getOwnerId() == chatId) {
                var textToSend = EmojiParser.parseToUnicode(messageText.substring(messageText.indexOf(" ")));
                var users = userRepository.findAll();
                for (ChatUser user: users){
                    sendMessageForUser.prepareAndSendMessage(user.getChatId(), textToSend);
                }
            } else {
                switch (messageText) {
                    case "/start":
                        register.registerUser(update.getMessage());
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                        break;
                    case "/help":
                        sendMessageForUser.prepareAndSendMessage(chatId, staticConstant.HELP_TEXT);
                        break;
                    case "/register":
                        register.register(chatId);
                        break;
                    default:
                        sendMessageForUser.prepareAndSendMessage(chatId, "Извините, пока я этого не умею" + " ");
                }
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            if(callbackData.equals(staticConstant.YES_BUTTON)){
                String text = "Да";
                sendMessageForUser.executeEditMessageText(text, chatId, messageId);
            } else if(callbackData.equals(staticConstant.NO_BUTTON)){
                String text = "Нет";
                sendMessageForUser.executeEditMessageText(text, chatId, messageId);
            }
        }
    }

    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    private void startCommandReceived(long chatId, String name) {
        String answer = "Здравствуйте, " + name + ", рада видеть Вас!"+ " :blush:";
        log.info("Replied to user " + name);
        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("Выбрать дату и время");
        row.add("Выбрать услугу");
        row.add("Помощь");
        keyboardRows.add(row);
        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);
        sendMessageForUser.executeMessage(message);
    }

}
