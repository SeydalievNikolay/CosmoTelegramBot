package com.example.CosmitologistsOffice.config;

import com.example.CosmitologistsOffice.service.impl.TelegramBotServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
public class BotInitializer {
    private final TelegramBotServiceImpl telegramBotService;


    @Autowired
    public BotInitializer(TelegramBotsApi telegramBotsApi, TelegramBotServiceImpl telegramBotService) {
        this.telegramBotService = telegramBotService;
        try {
            telegramBotsApi.registerBot(telegramBotService);
        } catch (TelegramApiException e) {
            log.error("Error occurred while registering bot: " + e.getMessage());
        }
    }

/*    @PostConstruct
    public void init() {
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(telegramBot);
        } catch (TelegramApiException e) {
            log.error("Error occurred while registering bot: " + e.getMessage());
        }
    }*/

    /*@Bean
    public TelegramBotService telegramBotService(BotLogicService botLogicService,
                                                 AppointmentService appointmentService,
                                                 ServicePriceProvider servicePriceProvider,
                                                 NotificationService notificationService,
                                                 AppointmentRepository appointmentRepository,
                                                 BotConfig config) {
        return new TelegramBotServiceImpl(botLogicService, appointmentService,
                servicePriceProvider, notificationService,
                appointmentRepository, config);
    }
    @Bean
    public TelegramMessageSender telegramMessageSender(TelegramBotService telegramBotService) {
        return new TelegramMessageSender(telegramBotService);
    }

    @Bean
    public BotLogicService botLogicService(ChatUserService chatUserService,
                                           MessageSender messageSender) {
        return new BotLogicServiceImpl(chatUserService, messageSender);
    }

    @Bean
    public AppointmentService appointmentService(AppointmentRepository appointmentRepository,
                                                 CosmetologistRepository cosmetologistRepository,
                                                 ServiceRepository serviceRepository,
                                                 ChatUserRepository chatUserRepository) {
        return new AppointmentServiceImpl(appointmentRepository,
                cosmetologistRepository, serviceRepository, chatUserRepository);
    }*/
}
