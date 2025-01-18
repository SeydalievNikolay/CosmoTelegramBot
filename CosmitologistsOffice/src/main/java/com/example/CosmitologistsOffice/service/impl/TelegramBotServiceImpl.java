package com.example.CosmitologistsOffice.service.impl;

import com.example.CosmitologistsOffice.config.BotConfig;
import com.example.CosmitologistsOffice.model.Appointment;
import com.example.CosmitologistsOffice.model.Service;
import com.example.CosmitologistsOffice.repository.AppointmentRepository;
import com.example.CosmitologistsOffice.repository.ServiceRepository;
import com.example.CosmitologistsOffice.service.*;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
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
public class TelegramBotServiceImpl extends TelegramLongPollingBot implements TelegramBotService {
    private final BotLogicService botLogicService;
    private final AppointmentService appointmentService;
    private final ServicePriceProvider servicePriceProvider;
    private final NotificationService notificationService;
    private final AppointmentRepository appointmentRepository;
    private final ServiceRepository serviceRepository;
    private final RegisterService registerService;
    final BotConfig config;

    public TelegramBotServiceImpl(BotLogicService botLogicService,
                                  AppointmentService appointmentService,
                                  ServicePriceProvider servicePriceProvider,
                                  NotificationService notificationService,
                                  AppointmentRepository appointmentRepository,
                                  ServiceRepository serviceRepository,
                                  RegisterService registerService,
                                  BotConfig config) {
        super(config.getToken());
        this.botLogicService = botLogicService;
        this.appointmentService = appointmentService;
        this.servicePriceProvider = servicePriceProvider;
        this.notificationService = notificationService;
        this.appointmentRepository = appointmentRepository;
        this.serviceRepository = serviceRepository;
        this.registerService = registerService;
        this.config = config;
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
        switch (messageText.toLowerCase()) {
            case "/start":
                log.info("Получена команда /start");
                String userName = update.getMessage().getFrom().getFirstName();
                String lastName = update.getMessage().getFrom().getLastName();
                String username = update.getMessage().getFrom().getUserName();

                botLogicService.startCommandReceived(chatId, userName, lastName, username);
                break;
            case "помощь":
                log.info("Получена команда помощь");
                botLogicService.sendHelpMessage(chatId, messageText);
                break;
            case "выбрать услугу":
                log.info("Получена команда выбрать_услугу");
                sendServiceOptions(chatId);
                handleSelectService(chatId, messageText);
                break;
            case "выбрать дату и время":
                log.info("Получена команда выбрать_дату_и_время");
                sendDateAndTimeSelection(chatId, appointment.getService());
                break;
            case "отменить или перенести запись":
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
                    botLogicService.sendErrorMessage(chatId, "Неизвестная команда. Пожалуйста, используйте кнопки или команды из меню.");
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

        String firstName = callbackQuery.getFrom().getFirstName();
        String lastName = callbackQuery.getFrom().getLastName();
        String username = callbackQuery.getFrom().getUserName();

        Appointment appointment = appointmentService.getOrCreateAppointment(chatId);

        switch (callbackData.toLowerCase()) {
            case "/start":
                log.info("Получена команда /start");
                botLogicService.startCommandReceived(chatId, firstName, lastName, username);
               /* registerService.registerUser((Message) callbackQuery.getMessage());*/
                break;
            case "service_":
                log.info("Получена команда service");
                sendServiceOptions(chatId);
                handleSelectService(chatId, callbackData);
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
                botLogicService.sendHelpMessage(chatId, "Помощь");
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
                break;
            case "record_yes":
                log.info("Получена команда RECORD_YES");
                appointmentService.recordAppointment(chatId);
                break;
            case "record_no":
                log.info("Получена команда RECORD_NO");
                botLogicService.sendSuccessMessage(chatId, "Хорошо, спасибо за понимание.");
                break;
            default:
                log.warn("Запись не найдена: {}", callbackData);
        }

        if (callbackData.startsWith("confirm_")) {
            long appointmentId = extractAppointmentId(callbackData);
            log.info("Подтверждена запись с ID: {}", appointmentId);
            confirmAppointment(chatId);
        } else if (callbackData.startsWith("cancel_")) {
            long appointmentId = extractAppointmentId(callbackData);
            log.info("Запись отменена с ID: {}", appointmentId);
            cancelAppointment(chatId);
        }
        if (callbackData.startsWith("select_date_time")) {
            handleDateSelection(callbackQuery.getMessage().getChatId(), callbackData);
        } else if (callbackData.startsWith("cancel_reschedule")) {
            handleTimeSelection(callbackQuery.getMessage().getChatId(), callbackData);
        }
        if (appointment.getDate() != null && appointment.getTime() != null && appointment.getService() != null) {
            notificationService.notifyCosmetologist(appointment);
        }
    }

    @Override
    public void execute(EditMessageText message) throws TelegramApiException {
        super.execute(message);
    }

    @Override
    public void execute(SendMessage message) throws TelegramApiException {
        super.execute(message);
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
            botLogicService.sendErrorMessage(chatId, "У вас нет активной записи для отмены или переноса.");
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

    private void handleSelectService(long chatId, String callbackData) {
        String serviceName = extractServiceName(callbackData);
        log.info("Выбрана услуга: {}", serviceName);

        Optional<Service> serviceOptional = serviceRepository.findByName(serviceName);
        if (serviceOptional.isPresent()) {
            Service service = serviceOptional.get();
            Appointment appointment = appointmentService.getOrCreateAppointment(chatId);
            appointment.setService(service.toString());
            appointmentRepository.save(appointment);

            // Отправляем сообщение с подтверждением
            sendConfirmation(chatId , appointment);
        } else {
            log.warn("Услуга не найдена: {}", serviceName);
            botLogicService.sendErrorMessage(chatId, "Услуга не найдена.");
        }
    }
    // Метод для извлечения названия услуги из callbackData
    private String extractServiceName(String callbackData) {
        if (callbackData.startsWith("service_")) {
            return callbackData.substring(7);
        } else {
            return callbackData;
        }
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
            Appointment appointment = appointmentService.getOrCreateAppointment(chatId);
            if (appointment == null) {
                botLogicService.sendErrorMessage(chatId, "У вас нет активной записи для подтверждения.");
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
                button.setCallbackData("select_date_time" + serviceName + "_" + date);
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
                botLogicService.sendErrorMessage(chatId, "К сожалению, у нас нет доступных дат для выбора.");
            }
        } catch (Exception e) {
            log.error("Ошибка при обработке выбора даты", e);
            botLogicService.sendErrorMessage(chatId, "Произошла ошибка при выборе даты. Попробуйте еще раз.");
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
        yesButton.setCallbackData("confirm_" + appointmentId);
        var noButton = new InlineKeyboardButton();
        noButton.setText("Нет");
        noButton.setCallbackData("cancel_" + appointmentId);
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

        appointment.setBooked(true);
        appointment.setBookedAt(new Timestamp(System.currentTimeMillis()));

        appointmentRepository.save(appointment);

        botLogicService.sendSuccessMessage(chatId, "Вы успешно записаны.");
    }




    private void cancelAppointment(long chatId) {
        Appointment appointment = appointmentService.getOrCreateAppointment(chatId);

        if (appointment == null || !appointment.isBooked()) {
            botLogicService.sendErrorMessage(chatId, "У вас нет активной записи для отмены.");
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
        row1.setCallbackData("record_yes");
        InlineKeyboardButton row2 = new InlineKeyboardButton();
        row1.setText("Нет");
        row1.setCallbackData("record_no");
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

    public void sendServiceOptions(long chatId) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        Map<String, BigDecimal> services = servicePriceProvider.getServicePrices();

        if (!services.isEmpty()) {
            List<InlineKeyboardButton> row = new ArrayList<>();

            for (Map.Entry<String, BigDecimal> entry : services.entrySet()) {
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(entry.getKey() + " (" + entry.getValue() + ")");
                button.setCallbackData("service_" + entry.getKey());
                row.add(button);
                keyboard.add(row);
                row = new ArrayList<>();
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
            botLogicService.sendErrorMessage(chatId, "К сожалению, список услуг пуст.");
        }
    }

    private long extractAppointmentId(String callbackData) {
        try {
            String[] parts = callbackData.split("_");
            return Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            log.error("Неверный формат callbackData для ID записи: {}", callbackData);
            throw new IllegalArgumentException("Неверный формат callbackData");
        }
    }

    // Логика выбора даты и времени
    public void sendDateAndTimeSelection(long chatId, String selectedService) {
        Appointment appointment = appointmentService.getOrCreateAppointment(chatId);
        if (appointment == null) {
            botLogicService.sendErrorMessage(chatId, "У вас нет активной записи для подтверждения.");
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

    private List<List<InlineKeyboardButton>> getListsButtonsForDate(long chatId) {
        Appointment appointment = appointmentService.getOrCreateAppointment(chatId);
        if (appointment == null) {
            log.warn("Пользователь не имеет активной записи");
            return Collections.emptyList();
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
            button.setCallbackData("select_date_" + serviceName + "_" + date);
            rows.add(Collections.singletonList(button));
        }

        return rows;
    }
}
