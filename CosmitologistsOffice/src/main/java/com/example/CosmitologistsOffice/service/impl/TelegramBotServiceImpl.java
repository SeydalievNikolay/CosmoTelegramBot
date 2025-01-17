package com.example.CosmitologistsOffice.service.impl;

import com.example.CosmitologistsOffice.config.BotConfig;
import com.example.CosmitologistsOffice.constants.StaticConstant;
import com.example.CosmitologistsOffice.exceptions.AppointmentNotFoundException;
import com.example.CosmitologistsOffice.model.Appointment;
import com.example.CosmitologistsOffice.repository.AppointmentRepository;
import com.example.CosmitologistsOffice.repository.ChatUserRepository;
import com.example.CosmitologistsOffice.repository.CosmetologistRepository;
import com.example.CosmitologistsOffice.repository.ServiceRepository;
import com.example.CosmitologistsOffice.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
public class TelegramBotServiceImpl extends TelegramLongPollingBot implements TelegramBotService {
    @Autowired
    private SendMessageForUserService sendMessageForUserService;
    @Autowired
    private AppointmentService appointmentService;
    @Autowired
    private ServicePriceProvider servicePriceProvider;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private ChatUserRepository chatUserRepository;
    @Autowired
    private ServiceRepository serviceRepository;
    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private CosmetologistRepository cosmetologistRepository;
    @Autowired
    private StaticConstant staticConstant;
    final BotConfig config;


    @Autowired
    public void setRegister(RegisterService register) {
    }

    @Autowired
    public void setUserRepository(ChatUserRepository chatUserRepository) {
        this.chatUserRepository = chatUserRepository;
    }

    @Autowired
    public void setStaticConstant(StaticConstant staticConstant) {
        this.staticConstant = staticConstant;
    }

    public TelegramBotServiceImpl(BotConfig botConfig) {
        this.config = botConfig;
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }


    @Override
    public void onUpdateReceived(Update update) {
        log.info("Получено обновление: {}", update);

        if (update.hasMessage()) {
            handleMessage(update);
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
        }
    }

    private void handleMessage(Update update) {
        // Логика обработки при получении сообщения
        log.debug("Получено текстовое сообщение");
        String messageText = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();
        log.debug("Полученный текст: {}, ID чата: {}", messageText, chatId);


        Appointment appointment = appointmentService.getOrCreateAppointment(chatId);

        log.debug("Начало обработки команды");
        SendMessageForUserService sendMessageService = new SendMessageForUserServiceImpl();
        sendMessageService.setTelegramBotService(this);

        switch (messageText.toLowerCase()) {
            case "/start":
                log.info("Получена команда /start");
                String userName = update.getMessage().getFrom().getFirstName();
                sendMessageService.startCommandReceived(chatId, userName);
                break;
            case "помощь":
                log.info("Получена команда помощь");
                sendMessageService.sendHelpMessage(chatId, messageText);
                break;
            case "выбрать_услугу":
                log.info("Получена команда выбрать_услугу");
                sendServiceOptions(chatId);
                break;
            case "выбрать_дату_и_время":
                log.info("Получена команда выбрать_дату_и_время");
                sendDateAndTimeSelection(chatId, appointment.getService());
                break;
            case "отменить_или_перенести_запись":
                log.info("Получена команда отменить_или_перенести_запись");
                handleCancelOrReschedule(chatId, appointment);
                break;
            default:
                if (messageText.startsWith("услуга: ")) {
                    String selectedService = messageText.substring(8).trim();
                    appointment.setService(selectedService);
                    sendDateAndTimeSelection(chatId, selectedService);
                } else if (messageText.matches("\\d{2}\\.\\d{2}\\.\\d{4}")) {
                    appointment.setDate(messageText);
                    sendTimeOptions(chatId, messageText);
                } else if (messageText.matches("\\d{2}:\\d{2}")) {
                    appointment.setTime(messageText);
                    sendConfirmation(chatId, appointment);
                } else {
                    sendMessageForUserService.sendErrorMessage(chatId, "Неизвестная команда. Пожалуйста, используйте кнопки или команды из меню.");
                }
        }

        appointmentRepository.save(appointment);

        if (appointment.getDate() != null && appointment.getTime() != null && appointment.getService() != null) {
            notificationService.notifyCosmetologist(appointment);
        }
    }

    @Override
    public void handleCallbackQuery(CallbackQuery callbackQuery) {
        // Логика обработки при нажатии на кнопки
        String callbackData = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();

        Appointment appointment = appointmentService.getOrCreateAppointment(chatId);

        switch (callbackData.toLowerCase()) {
            case "/start":
                log.info("Получена команда /start");
                sendMessageForUserService.startCommandReceived(chatId, callbackQuery.getFrom().getFirstName());
                break;
            case "select_service":
                log.info("Получена команда select_service");
                sendServiceOptions(chatId);
                break;
            case "select_date_time":
                log.info("Получена команда select_date_time");
                sendDateAndTimeSelection(chatId, appointment.getService());
                break;
            case "cancel_reschedule":
                log.info("Получена команда cancel_reschedule");
                cancelAppointment(chatId);
                break;
            case "show_help":
                log.info("Получена команда show_help");
                sendMessageForUserService.sendHelpMessage(chatId, "Помощь");
                break;
            case "cancel_appointment":
            case "no_button":
                log.info("Получена команда cancel_appointment, no_button");
                cancelAppointment(chatId);
                break;
            case "reschedule_appointment":
                log.info("Получена команда reschedule_appointment");
                rescheduleAppointment(chatId, callbackQuery.getInlineMessageId());
                break;
            case "yes_button":
                log.info("Получена команда yes_button");
                confirmAppointment(chatId);
                appointmentRepository.save(appointment);
                notificationService.notifyCosmetologist(appointment);
                break;
            case "record_yes":
                log.info("Получена команда RECORD_YES");
                appointmentService.recordAppointment(chatId);
                break;
            case "record_no":
                log.info("Получена команда RECORD_NO");
                sendMessageForUserService.sendSuccessMessage(chatId, "Хорошо, спасибо за понимание.");
                break;
            default:
                log.warn("Запись не найдена: {}", callbackData);
        }

        if (callbackData.startsWith("select_date_time")) {
            handleDateSelection(callbackQuery.getMessage().getChatId(), callbackData);
        } else if (callbackData.startsWith("cancel_reschedule")) {
            handleTimeSelection(callbackQuery.getMessage().getChatId(), callbackData);
        }
    }

    private void handleCancelOrReschedule(long chatId, Appointment appointment) {
        if (appointmentService.isBooked(appointment)) {
            InlineKeyboardMarkup inlineKeyboardMarkup = getInlineKeyboardMarkup(appointment);

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(String.valueOf(chatId));
            sendMessage.setText("Что вы хотите сделать?");
            sendMessage.setReplyMarkup(inlineKeyboardMarkup);

            try {
                execute(sendMessage);
            } catch (TelegramApiException e) {
                log.error("Ошибка при отправке сообщения для отмены или переноса записи", e);
            }
        } else {
            sendMessageForUserService.sendErrorMessage(chatId, "У вас нет активной записи для отмены или переноса.");
        }
    }

    private static InlineKeyboardMarkup getInlineKeyboardMarkup(Appointment appointment) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText("Отменить запись");
        cancelButton.setCallbackData("cancel_appointment" + appointment.getId());

        InlineKeyboardButton rescheduleButton = new InlineKeyboardButton();
        rescheduleButton.setText("Перенести запись");
        rescheduleButton.setCallbackData("reschedule_appointment" + appointment.getId());

        row.add(cancelButton);
        row.add(rescheduleButton);
        rows.add(row);

        inlineKeyboardMarkup.setKeyboard(rows);
        return inlineKeyboardMarkup;
    }

    private void rescheduleAppointment(long chatId, String messageId) {
        Appointment appointment = appointmentRepository.findById(chatId).orElse(null);
        if (appointment != null && appointment.isBooked()) {
            appointment.setBookedAt(null);
            appointment.setDate(null);
            appointment.setTime(null);
            appointmentRepository.save(appointment);

            sendServiceOptions(chatId);
        }
    }


    private void handleServiceSelection(long chatId, String callbackData) {
        String serviceName = extractServiceName(callbackData);
        BigDecimal price = servicePriceProvider.getServicePrice(serviceName);

        if (price.compareTo(BigDecimal.ZERO) > 0) {
            Appointment appointment = appointmentRepository.findByChatId(chatId);

            if (appointment == null) {
                appointment = new Appointment();
                appointment.setChatId(chatId);
            }

            appointment.setService(serviceName);
            appointment.setPrice(price);
            appointmentRepository.save(appointment);

            sendMessageForUserService.sendSuccessMessage(chatId, "Услуга '" + serviceName + "' выбрана. Цена: " + price);

            sendCalendarDates(chatId);
        } else {
            sendMessageForUserService.sendErrorMessage(chatId, "Ошибка при выборе услуги.");
        }
    }

    private String extractServiceName(String callbackData) {
        int index = callbackData.indexOf('_');
        if (index < 0 || index >= callbackData.length() - 1) {
            throw new IllegalArgumentException("Неверный формат callback-data для выбора услуги: " + callbackData);
        }
        return callbackData.substring(index + 1);
    }


    /*    private void handleDateSelection(long chatId, String callbackData) {
            String selectedDate = callbackData.split("_")[1];
            Appointment appointment = appointmentRepository.findByChatId(chatId);
            appointment.setDate(selectedDate);
            appointmentRepository.save(appointment);

            sendTimeOptions(chatId, selectedDate);
        }*/
    private void handleDateSelection(long chatId, String callbackData) {
        try {
            Appointment appointment = appointmentService.getAppointment(chatId);
            if (appointment == null) {
                sendMessageForUserService.sendErrorMessage(chatId, "У вас нет активной записи для подтверждения.");
                return;
            }

            String serviceName = appointment.getService();
            LocalDate currentDate = LocalDate.now();

            List<LocalDate> dates = IntStream.range(0, 7)
                    .mapToObj(currentDate::plusDays)
                    .toList();

            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            for (LocalDate date : dates) {
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(date.toString());
                button.setCallbackData("select_date" + serviceName + "_" + date);
                rows.add(Collections.singletonList(button));
            }

            if (!rows.isEmpty()) {
                InlineKeyboardMarkup markup = new InlineKeyboardMarkup(rows);
                SendMessage message = new SendMessage();
                message.setChatId(String.valueOf(chatId));
                message.setText("Выберите дату и время для вашей записи:");
                message.setReplyMarkup(markup);

                execute(message);
            } else {
                sendMessageForUserService.sendErrorMessage(chatId, "К сожалению, у нас нет доступных дат для выбора.");
            }
        } catch (Exception e) {
            log.error("Ошибка при обработке выбора даты", e);
            sendMessageForUserService.sendErrorMessage(chatId, "Произошла ошибка при выборе даты. Попробуйте еще раз.");
        }
    }


    private void handleTimeSelection(long chatId, String callbackData) {
        String selectedTime = callbackData.split("_")[1];
        Appointment appointment = appointmentRepository.findByChatId(chatId);
        appointment.setTime(selectedTime);
        appointmentRepository.save(appointment);

        sendConfirmation(chatId, appointment);
    }


    private void sendConfirmation(long chatId, Appointment appointment) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Вы выбрали услугу: " + appointment.getService() +
                ", на дату: " + appointment.getDate() + " в "
                + appointment.getTime() + ". Подтвердите запись.");
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = getListsButtonsForConfirmation(appointment.getId());
        inlineKeyboardMarkup.setKeyboard(rowsInLine);
        message.setReplyMarkup(inlineKeyboardMarkup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения с подтверждением", e);
        }
    }

    private List<List<InlineKeyboardButton>> getListsButtonsForConfirmation(Long appointmentId) {
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        var yesButton = new InlineKeyboardButton();
        yesButton.setText("Да");
        yesButton.setCallbackData("CONFIRM_" + appointmentId);
        var noButton = new InlineKeyboardButton();
        noButton.setText("Нет");
        noButton.setCallbackData("CANCEL_" + appointmentId);
        rowInLine.add(yesButton);
        rowInLine.add(noButton);
        rowsInLine.add(rowInLine);
        return rowsInLine;
    }


    private void confirmAppointment(long chatId) {
        Appointment appointment = appointmentRepository.findByChatId(chatId);
        if (appointment == null) {
            log.error("Запись на прием по chatId не найдена: {}", chatId);
            return;
        }

        // Обновляем статус записи как подтвержденную
        appointment.setBooked(true);
        appointment.setBookedAt(new Timestamp(System.currentTimeMillis()));

        // Сохраняем обновления в базе данных
        appointmentRepository.save(appointment);

        sendMessageForUserService.sendSuccessMessage(chatId, "Вы успешно записаны.");

        notificationService.notifyCosmetologist(appointment);
    }


    private void cancelAppointment(long chatId) {
        Appointment appointment = appointmentService.getAppointment(chatId);

        if (appointment == null || !appointment.isBooked()) {
            sendMessageForUserService.sendErrorMessage(chatId, "У вас нет активной записи для отмены.");
        } else {
            appointment.setBookedAt(null);
            appointment.setDate(null);
            appointment.setTime(null);
            appointmentRepository.save(appointment);

            SendMessage message = getSendMessageRecordingCancelled(chatId);
            try {
                execute(message);
            } catch (TelegramApiException e) {
                log.error("Ошибка при отправке сообщения после отмены записи", e);
            }
        }
    }

    private static SendMessage getSendMessageRecordingCancelled(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Ваша запись отменена.\nХотите записаться на другое время?");

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        InlineKeyboardButton row1 = new InlineKeyboardButton();
        row1.setText("Да");
        row1.setCallbackData("RECORD_YES");
        InlineKeyboardButton row2 = new InlineKeyboardButton();
        row1.setText("Нет");
        row1.setCallbackData("RECORD_NO");
        List<InlineKeyboardButton> rows = new ArrayList<>();
        rows.add(row1);
        rows.add(row2);

        keyboard.add(rows);

        inlineKeyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(inlineKeyboardMarkup);

        return message;
    }

    private void sendCalendarDates(long chatId) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        LocalDate today = LocalDate.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        for (int i = 0; i < 30; i++) {
            LocalDate date = today.plusDays(i);
            List<InlineKeyboardButton> listRows = new ArrayList<>();
            InlineKeyboardButton row = new InlineKeyboardButton();
            row.setText(date.format(dateFormatter));
            row.setCallbackData("DATE_" + date);
            listRows.add(row);
            keyboard.add(listRows);
        }

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выберите дату:");
        message.setReplyMarkup(inlineKeyboardMarkup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения с датами календаря", e);
        }
    }

    private void sendTimeOptions(long chatId, String selectedDate) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<String> availableTimes = getAvailableTimeSlots(selectedDate);

        for (String time : availableTimes) {
            List<InlineKeyboardButton> listRows = new ArrayList<>();
            InlineKeyboardButton row = new InlineKeyboardButton();
            row.setText(time);
            row.setCallbackData("TIME_" + time);

            listRows.add(row);
            keyboard.add(listRows);
        }

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выберите доступное время:");
        message.setReplyMarkup(inlineKeyboardMarkup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения с опциями времени", e);
        }
    }

    private List<String> getAvailableTimeSlots(String date) {
        List<Appointment> appointments = appointmentRepository.findByDate(date);
        Set<String> takenTimes = appointments.stream()
                .map(Appointment::getTime)
                .collect(Collectors.toSet());

        List<String> availableTimes = new ArrayList<>();
        LocalTime time = LocalTime.of(10, 0);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        for (int i = 0; i < 5; i++) {
            String formattedTime = time.format(timeFormatter);
            if (!takenTimes.contains(formattedTime)) {
                availableTimes.add(formattedTime);
            }
            time = time.plusHours(2);
        }

        return availableTimes;
    }


    private void sendServiceOptions(long chatId) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        Map<String, BigDecimal> services = servicePriceProvider.getServicePrices();

        if (!services.isEmpty()) {
            for (Map.Entry<String, BigDecimal> entry : services.entrySet()) {
                List<InlineKeyboardButton> row = new ArrayList<>();
                for (int i = 0; i < 1; i++) {
                    InlineKeyboardButton button = new InlineKeyboardButton();
                    button.setText(entry.getKey() + " (" + entry.getValue() + ")");
                    button.setCallbackData("select_service_" + entry.getKey());
                    row.add(button);
                }
                keyboard.add(row);
            }

            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(keyboard);

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(String.valueOf(chatId));
            sendMessage.setText("Выберите услугу:");
            sendMessage.setReplyMarkup(inlineKeyboardMarkup);

            try {
                execute(sendMessage);
                log.info("Отправлено сообщение с выбором услуги");
            } catch (TelegramApiException e) {
                log.error("Ошибка при отправке списка услуг", e);
            }
        } else {
            sendMessageForUserService.sendErrorMessage(chatId, "К сожалению, список услуг пуст.");
        }
    }


    private void sendDateAndTimeSelection(long chatId, String selectedService) {
        Appointment appointment = appointmentService.getOrCreateAppointment(chatId);
        if (appointment == null) {
            sendMessageForUserService.sendErrorMessage(chatId, "У вас нет активной записи для подтверждения.");
        } else {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText("Вы выбрали услугу: " + selectedService + ". Теперь выберите дату.");

            InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rowsInLine = getListsButtonsForDate(chatId);
            markupInLine.setKeyboard(rowsInLine);
            message.setReplyMarkup(markupInLine);

            try {
                execute(message);
            } catch (TelegramApiException e) {
                log.error("Ошибка при отправке сообщения с выбором даты", e);
            }
        }
    }

    /*    private List<List<InlineKeyboardButton>> getListsButtonsForDate() {
            List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
            List<LocalDate> dates = getNextFiveDays();

            for (int i = 0; i < dates.size(); i += 3) {
                List<InlineKeyboardButton> row = new ArrayList<>();
                for (int j = i; j < Math.min(i + 3, dates.size()); j++) {
                    InlineKeyboardButton button = new InlineKeyboardButton();
                    button.setText(dates.get(j).format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
                    button.setCallbackData("DATE_" + dates.get(j).toString());
                    row.add(button);
                }
                rowsInLine.add(row);
            }

            return rowsInLine;
        }*/

    private List<List<InlineKeyboardButton>> getListsButtonsForDate(long chatId) {
        try {
            Appointment appointment = appointmentService.getAppointment(chatId);
            if (appointment == null) {
                throw new AppointmentNotFoundException("У пользователя нет активной записи");
            }
            String serviceName = appointment.getService();
            LocalDate currentDate = LocalDate.now();

            List<LocalDate> dates = IntStream.range(0, 7)
                    .mapToObj(currentDate::plusDays)
                    .toList();

            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            for (LocalDate date : dates) {
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(date.toString());
                button.setCallbackData("select_date" + serviceName + "_" + date);
                rows.add(Collections.singletonList(button));
            }

            return rows;
        } catch (AppointmentNotFoundException e) {
            log.warn("Пользователь не имеет активной записи", e);
            return Collections.emptyList();
        }
    }


}
