package com.example.CosmitologistsOffice.service.impl;

import com.example.CosmitologistsOffice.config.BotConfig;
import com.example.CosmitologistsOffice.constants.StaticConstant;
import com.example.CosmitologistsOffice.exceptions.AppointmentNotFoundException;
import com.example.CosmitologistsOffice.model.Appointment;
import com.example.CosmitologistsOffice.model.Cosmetologist;
import com.example.CosmitologistsOffice.repository.AppointmentRepository;
import com.example.CosmitologistsOffice.repository.CosmetologistRepository;
import com.example.CosmitologistsOffice.service.*;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Contact;
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
    private final CosmetologistRepository cosmetologistRepository;
    private final RegisterService registerService;
    private final ChatUserService chatUserService;
    final BotConfig config;

    public TelegramBotServiceImpl(BotLogicService botLogicService,
                                  AppointmentService appointmentService,
                                  ServicePriceProvider servicePriceProvider,
                                  NotificationService notificationService,
                                  AppointmentRepository appointmentRepository,
                                  CosmetologistRepository cosmetologistRepository,
                                  RegisterService registerService,
                                  ChatUserService chatUserService,
                                  BotConfig config) {
        super(config.getToken());
        this.botLogicService = botLogicService;
        this.appointmentService = appointmentService;
        this.servicePriceProvider = servicePriceProvider;
        this.notificationService = notificationService;
        this.appointmentRepository = appointmentRepository;
        this.cosmetologistRepository = cosmetologistRepository;
        this.registerService = registerService;
        this.chatUserService = chatUserService;
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

    /**
     * Этот метод вызывается, когда бот получает обновление от Telegram. Если обновление содержит сообщение,
     * вызывается метод для обработки сообщения. Если обновление содержит callback-запрос, вызывается метод для обработки callback-запроса.
     *
     * @param update Обновление, полученное от Telegram, которое содержит информацию о новом сообщении,
     *               callback-запросе или других действиях пользователя.
     *
     * @throws RuntimeException Если происходит ошибка при обработке сообщения, выбрасывается исключение RuntimeException.
     */
    @Override
    public void onUpdateReceived(Update update) {
        log.info("Получено обновление: {}", update);

        if (update.hasMessage()) {
            try {
                handleMessage(update);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
        }
    }

    /**
     * Обрабатывает входящие сообщения пользователя.
     *
     * @param update Объект Update, содержащий информацию о полученном сообщении.
     */
    private void handleMessage(Update update) throws TelegramApiException {
        log.debug("Получено текстовое сообщение");

        String messageText = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();
        log.debug("Полученный текст: {}, ID чата: {}", messageText, chatId);

        Appointment appointment = appointmentService.getOrCreateAppointment(chatId);

        log.info("Начало обработки команды");

        if (messageText.equalsIgnoreCase("/start")) {
            log.info("Получена команда /start");
            String firstName = update.getMessage().getFrom().getFirstName();
            String lastName = update.getMessage().getFrom().getLastName();
            String username = update.getMessage().getFrom().getUserName();

            Contact contact = update.getMessage().getContact();
            String phoneNumber = null;
            if (contact != null) {
                phoneNumber = contact.getPhoneNumber();
            } else {
                log.warn("Контактные данные отсутствуют при получении команды /start");
            }
            botLogicService.startCommandReceived(chatId, firstName, lastName, username, phoneNumber);
        } else if (messageText.equalsIgnoreCase("/register")) {
            log.info("Получена команда /register");
            registerService.register(chatId);
        } else if (messageText.equalsIgnoreCase("/help")) {
            log.info("Получена команда /help");
            botLogicService.sendHelpMessage(chatId, "Помощь");
        } else if (messageText.matches("^\\+7\\d{10}$")) {
            chatUserService.existPhoneNumber(chatId, messageText);
            registerService.sendPhoneNumberToCosmetologist(chatId, messageText);
            notificationService.notifyingTheCosmetologistWhenRegisteringAUser(chatId);
        } else {
            log.warn("Неизвестная команда: {}", update.getMessage());
            botLogicService.sendErrorMessage(chatId, "Извините, произошла ошибка, уже работаем по её устранению.");
        }

        appointmentRepository.save(appointment);
    }

    /**
     * Обрабатывает входящие callback запросы пользователя.
     *
     * @param callbackQuery Объект CallbackQuery, содержащий информацию о нажатии кнопки.
     */
    @Override
    public void handleCallbackQuery(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();
        String firstName = callbackQuery.getFrom().getFirstName();
        String lastName = callbackQuery.getFrom().getLastName();
        String username = callbackQuery.getFrom().getUserName();

        Appointment appointment = appointmentService.getOrCreateAppointment(chatId);

        String phoneNumber = chatUserService.getPhoneNumberByChatId(chatId);
        log.info("Получен callbackData: {}", callbackData);

        if (callbackData != null && isService(callbackData)) {
            appointment.setService(callbackData);
            appointmentRepository.save(appointment);
            getListsButtonsForDate(chatId);
        } else if (callbackData.equals("/start")) {
            log.info("Пользователь нажал /start");
            botLogicService.startCommandReceived(chatId, firstName, lastName, username,phoneNumber );
        } else if (callbackData.startsWith("select_date_")) {
            String[] parts = callbackData.split("_");
            String selectedDate = parts[2];
            log.info("Пользователь выбрал дату: {}", selectedDate);
            appointment.setDate(selectedDate);
            appointmentRepository.save(appointment);
            sendTimeOptions(callbackQuery.getMessage().getChatId(), selectedDate);
        } else if (callbackData.startsWith("time_")) {
            String selectedTime = callbackData.substring(5);
            log.info("Пользователь выбрал время: {}", selectedTime);
            appointment.setTime(selectedTime);
            appointmentRepository.save(appointment);
            sendConfirmation(chatId, appointment);
        } else if (callbackData.startsWith("cancel_or_reschedule_recording_")) {
            log.info("Получена команда cancel_or_reschedule_recording_");
            processCancelOrRescheduleRequest(chatId, callbackData);
        } else if (callbackData.startsWith("cancel_appointment_") || callbackData.equals("no_button_")) {
            log.info("Получена команда cancel_appointment_, no_button_");
            long appointmentId = extractAppointmentIdFromMenu(callbackData);
            cancelAppointment(chatId, appointmentId);
        } else if (callbackData.startsWith("reschedule_appointment_")) {
            log.info("Получена команда reschedule_appointment_");
            long appointmentId = extractAppointmentIdFromMenu(callbackData);
            rescheduleAppointment(chatId, appointmentId);
        } else if (callbackData.equals("cancel_")) {
            long appointmentId = extractAppointmentId(callbackData);
            cancelAppointment(chatId, appointmentId);
            log.info("Пользователь отменил запись с ID: {}", appointmentId);
        } else if (callbackData.startsWith("confirm_")) {
            long appointmentId = extractAppointmentId(callbackData);
            confirmAppointment(chatId);
            log.info("Пользователь подтвердил запись с ID: {}", appointmentId);
        } else if (callbackData.startsWith("select_service")) {
            log.info("Получена команда select_service");
            sendServiceOptions(chatId);
        } else if (callbackData.startsWith("show_help")) {
            log.info("Получена команда show_help");
            botLogicService.sendHelpMessage(chatId, "Помощь");
        } else if (callbackData.equals("yes_button")) {
            log.info("Получена команда yes_button");
            confirmAppointment(chatId);
            appointmentRepository.save(appointment);
        } else if (callbackData.equals("record_yes")) {
            log.info("Получена команда record_yes");
            sendServiceOptions(chatId);
        } else if (callbackData.equals("record_no")) {
            log.info("Получена команда record_no");
            botLogicService.sendSuccessMessage(chatId, "Хорошо " + firstName + " , будем вас ждать в следующий раз.");
        } else if (callbackData.equals(StaticConstant.YES_BUTTON_SEND_PHONE_NUMBER)) {
            log.info("Пользователь подтвердил регистрацию");
            registerService.registerUser(chatId);
        } else if (callbackData.equals(StaticConstant.NO_BUTTON_SEND_PHONE_NUMBER)) {
            log.info("Пользователь отменил регистрацию");
            registerService.sendCancelMessage(chatId);
        } else if (callbackData.equals(StaticConstant.CANCEL_REGISTRATION)) {
            log.info("Пользователь отменил регистрацию");
            registerService.sendCancelMessage(chatId);
        } else {
            log.warn("Неизвестная команда из меню : {}", callbackData);
            botLogicService.sendErrorMessage(chatId, "Извините, произошла ошибка, уже работаем по её устранению.");
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

    /**
     * Создает клавиатуру для выбора действия при отмене или переносе записи.
     *
     * @param appointmentId Запись на прием
     * @return InlineKeyboardMarkup объект
     */
    private static InlineKeyboardMarkup getInlineKeyboardMarkup(long appointmentId) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText("Отменить запись");
        cancelButton.setCallbackData("cancel_appointment_" + appointmentId);

        InlineKeyboardButton rescheduleButton = new InlineKeyboardButton();
        rescheduleButton.setText("Перенести запись");
        rescheduleButton.setCallbackData("reschedule_appointment_" + appointmentId);

        row.add(cancelButton);
        row.add(rescheduleButton);
        rows.add(row);

        inlineKeyboardMarkup.setKeyboard(rows);
        return inlineKeyboardMarkup;
    }

    /**
     *Этот метод выполняет проверку наличия активной записи для пользователя по его chatId. Если активная запись существует,
     * он отправляет пользователю сообщение с возможностью отмены или переноса этой записи. Если записи нет,
     * пользователю отправляется сообщение об ошибке.
     *
     * @param chatId Идентификатор чата пользователя, для которого обрабатывается запрос.
     * @param callbackData Данные, передаваемые с callback запросом от пользователя, которые могут указывать на действие,
     *                     например, отмену или перенос записи.
     */
    private void processCancelOrRescheduleRequest(long chatId, String callbackData) {
        Appointment appointment = appointmentRepository.findByChatId(chatId);

        if (appointment == null || !appointment.isBooked()) {
            botLogicService.sendErrorMessage(chatId, "У вас нет активной записи для отмены или переноса.");
            return;
        }

        InlineKeyboardMarkup inlineKeyboardMarkup = getInlineKeyboardMarkup(appointment.getId());
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выберите действие:");
        message.setReplyMarkup(inlineKeyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения с вариантами отмены или переноса записи", e);
        }
    }

    /**
     * Извлекает ID записи из callbackData.
     *
     * @param callbackData Данные callback-объекта
     * @return ID записи в виде long
     */
    private long extractAppointmentId(String callbackData) {
        try {
            String[] parts = callbackData.split("_");

            if (parts.length > 1) {
                return Long.parseLong(parts[1]);
            }
        } catch (NumberFormatException e) {
            log.error("Неверный формат callbackData для ID записи: {}", callbackData);
            throw new IllegalArgumentException("Неверный формат callbackData" + callbackData);
        }
        return 0;
    }

    /**
     * Извлекает ID записи из callbackData.
     *
     * @param callbackData Данные callback-объекта
     * @return ID записи в виде long
     */
    private long extractAppointmentIdFromMenu(String callbackData) {
        try {
            String[] parts = callbackData.split("_");
            if (parts.length > 2 && parts[0].equals("cancel") && parts[1].equals("appointment")) {

                return Long.parseLong(parts[2]);
            }
            if (parts.length > 2 && parts[0].equals("no") && parts[1].equals("button")) {

                return Long.parseLong(parts[2]);
            }
        } catch (NumberFormatException e) {
            log.error("Неверный формат callbackData для ID записи из меню: {}", callbackData);
            throw new IllegalArgumentException("Неверный формат callbackData из меню");
        }
        return 0;
    }

    /**
     * Переносит существующую запись на прием.
     *
     * @param chatId        ID чата пользователя
     * @param appointmentId ID записи, к которому привязан callback
     */
    private void rescheduleAppointment(long chatId, long appointmentId) {
        Appointment appointment = appointmentRepository.findById(chatId)
                .orElseThrow(()-> new AppointmentNotFoundException("Запись не найдена  "));

        if (appointment != null && appointment.isBooked()) {
            appointment.setBookedAt(null);
            appointment.setDate(null);
            appointment.setTime(null);
            appointmentRepository.save(appointment);

            sendServiceOptions(chatId);
        } else {

            log.error("Не удалось найти активную запись для переноса или отмены");
            botLogicService.sendErrorMessage(chatId, "Запись не найдена для переноса.");
        }
    }

    /**
     * Отправляет сообщение с подтверждением выбранных параметров записи на прием.
     *
     * @param chatId      ID чата пользователя
     * @param appointment Объект записи на прием с выбранными параметрами
     */
    private void sendConfirmation(long chatId, Appointment appointment) {
        String messageText = String.format("Вы выбрали услугу: %s.\nНа дату: %s\nВремя: %s\nПодтвердите запись.",
                appointment.getService(),
                appointment.getDate(),
                appointment.getTime());

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(messageText);

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = getListsButtonsForConfirmation(appointment.getId());

        List<List<InlineKeyboardButton>> splitRows = new ArrayList<>();
        for (int i = 0; i < rowsInLine.get(0).size(); i++) {
            List<InlineKeyboardButton> newRow = new ArrayList<>();
            newRow.add(rowsInLine.get(0).get(i));
            splitRows.add(newRow);
        }

        markupInLine.setKeyboard(splitRows);
        message.setReplyMarkup(markupInLine);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения с подтверждением", e);
        }
    }

    /**
     * Создает список кнопок для подтверждения записи на прием.
     *
     * @param appointmentId ID записи на прием
     * @return Список списков кнопок для инлайн-клавиатуры
     */
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

    /**
     * Подтверждает запись на прием и устанавливает статус "забронировано".
     *
     * @param chatId ID чата пользователя
     */
    private void confirmAppointment(long chatId) {
        Appointment appointment = appointmentRepository.findByChatId(chatId);
        if (appointment == null) {
            log.error("Запись на прием по chatId не найдена: {}", chatId);
            return;
        }
        if (appointment.getCosmetologist() == null) {

            Cosmetologist cosmetologist = cosmetologistRepository.findById(1L)
                    .orElseThrow(() -> new RuntimeException("Косметолог не найден для записи."));

            appointment.setCosmetologist(cosmetologist);

            log.info("Косметолог назначен на запись с chatId: {}. Косметолог: {}", chatId, cosmetologist.getName());
        }

        appointment.setBooked(true);

        appointment.setBookedAt(new Timestamp(System.currentTimeMillis()));

        appointmentRepository.save(appointment);

        botLogicService.sendSuccessMessage(chatId, "Вы успешно записаны. Александра скоро с вами свяжется");

        notificationService.notifyCosmetologist(appointment);
    }

    /**
     * Отменяет существующую запись на прием.
     *
     * @param chatId ID чата пользователя
     */
    private void cancelAppointment(long chatId, Long appointmentId) {
        Appointment appointment;
        log.info("Если appointmentId не null, используем его для поиска записи");

        if (appointmentId != null) {
            appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new AppointmentNotFoundException("Запись не найдена "));
        } else {
            log.info("Если appointmentId null, используем chatId для поиска активной записи");
            appointment = appointmentService.getOrCreateAppointment(chatId);
        }

        if (!appointment.isBooked()) {
            botLogicService.sendErrorMessage(chatId, "У вас нет активной записи для отмены.");
        } else {
            log.info("Запись с ID {} была удалена", appointmentId);
            notificationService.notifyCosmetologistForCanselAppointment(chatId, appointmentId);

            SendMessage message = getSendMessageRecordingCancelled(chatId);
            try {
                execute(message);
            } catch (TelegramApiException e) {
                log.error("Ошибка при отправке сообщения после отмены записи", e);
            }
        }
    }

    /**
     * Создает сообщение для отправки после отмены записи.
     *
     * @param chatId ID чата пользователя
     * @return SendMessage объект с текстом и клавиатурой для повторного бронирования
     */
    private static SendMessage getSendMessageRecordingCancelled(long chatId) {
        log.info("Отправка кнопок с выбором записи на другое время ");

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Ваша запись отменена.\nХотите записаться на другое время?");

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        InlineKeyboardButton row1 = new InlineKeyboardButton();
        row1.setText("Да");
        row1.setCallbackData("record_yes");

        InlineKeyboardButton row2 = new InlineKeyboardButton();
        row2.setText("Нет");
        row2.setCallbackData("record_no");

        List<InlineKeyboardButton> rows = new ArrayList<>();
        rows.add(row1);
        rows.add(row2);

        keyboard.add(rows);

        inlineKeyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(inlineKeyboardMarkup);

        return message;
    }


    /**
     * Отправляет пользователю доступные временные слоты для выбранной даты.
     *
     * @param chatId       ID чата пользователя
     * @param selectedDate Выбранная дата в формате строки
     */
    private void sendTimeOptions(long chatId, String selectedDate) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<String> availableTimes = getAvailableTimeSlots(selectedDate);

        log.info("Получен список доступных времен для даты {}: {}", selectedDate, availableTimes);

        if (availableTimes.isEmpty()) {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText("К сожалению, в выбранную дату нет доступных времен.");
            try {
                execute(message);
            } catch (TelegramApiException e) {
                log.error("Ошибка при отправке сообщения о отсутствии доступных времен", e);
            }
            return;
        }

        for (String time : availableTimes) {
            List<InlineKeyboardButton> listRows = new ArrayList<>();
            InlineKeyboardButton row = new InlineKeyboardButton();
            row.setText(time);
            row.setCallbackData("time_" + time);

            listRows.add(row);
            keyboard.add(listRows);
        }

        inlineKeyboardMarkup.setKeyboard(keyboard);
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выберите доступное время:");
        message.setReplyMarkup(inlineKeyboardMarkup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке списка времени", e);
        }
    }


    /**
     * Получает список доступных временных слотов для заданной даты.
     *
     * @param date Дата в формате строки
     * @return Список строковых представлений доступных времен
     */
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

    /**
     * Создает клавиатуру с кнопками для выбора опций.
     *
     * @param options Список пар значений и callbackData для кнопок
     * @return InlineKeyboardMarkup объект с подготовленной клавиатурой
     */
    private InlineKeyboardMarkup createOptionKeyboard(List<Map.Entry<String, String>> options) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (Map.Entry<String, String> option : options) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(option.getKey());
            button.setCallbackData(option.getValue());

            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            keyboard.add(row);
        }

        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }

    /**
     * Отправляет пользователю список доступных услуг с ценами.
     *
     * @param chatId ID чата пользователя
     */
    public void sendServiceOptions(long chatId) {
        Map<String, BigDecimal> services = servicePriceProvider.getServicePrices();
        log.info("Получен прайс лист улуг");
        if (!services.isEmpty()) {
            List<Map.Entry<String, String>> options = services.entrySet().stream()
                    .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey() + " (" + entry.getValue() + ")", entry.getKey()))
                    .collect(Collectors.toList());

            InlineKeyboardMarkup inlineKeyboardMarkup = createOptionKeyboard(options);

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


    /**
     * Создает список кнопок для выбора даты записи на прием.
     *
     * @param chatId ID чата пользователя
     */
    private void getListsButtonsForDate(long chatId) {
        Appointment appointment = appointmentService.getOrCreateAppointment(chatId);
        if (appointment == null) {
            log.warn("Пользователь не имеет активной записи");
            return;
        }

        String serviceName = appointment.getService();
        LocalDate currentDate = LocalDate.now();

        List<LocalDate> dates = IntStream.range(0, 7)
                .mapToObj(currentDate::plusDays)
                .toList();

        List<Map.Entry<String, String>> options = dates.stream()
                .map(date -> new AbstractMap.SimpleEntry<>(date.toString(), "select_date_" + date))
                .collect(Collectors.toList());

        InlineKeyboardMarkup inlineKeyboardMarkup = createOptionKeyboard(options);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Вы выбрали услугу: " + serviceName + ". Теперь выберите дату.");
        message.setReplyMarkup(inlineKeyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения с выбором даты", e);
        }

    }

    /**
     * Этот метод проверяет, содержится ли название услуги (callbackData) в списке доступных услуг,
     * предоставляемых через {@link ServicePriceProvider}. Он использует метод {@link ServicePriceProvider#getServicePrices()},
     * чтобы получить все доступные услуги и их цены, и проверяет, существует ли в этом списке переданное название услуги.
     *
     * @param callbackData Название услуги или идентификатор услуги, переданный в callbackData.
     * @return {@code true}, если callbackData соответствует одной из услуг с установленной ценой; {@code false} в противном случае.
     */
    private boolean isService(String callbackData) {
        return servicePriceProvider.getServicePrices().containsKey(callbackData);
    }

}
