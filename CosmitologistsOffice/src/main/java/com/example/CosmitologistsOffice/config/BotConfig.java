package com.example.CosmitologistsOffice.config;

import com.example.CosmitologistsOffice.repository.AppointmentRepository;
import com.example.CosmitologistsOffice.repository.ChatUserRepository;
import com.example.CosmitologistsOffice.repository.CosmetologistRepository;
import com.example.CosmitologistsOffice.repository.ServiceRepository;
import com.example.CosmitologistsOffice.service.*;
import com.example.CosmitologistsOffice.service.impl.*;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.PropertySource;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
@Data
@PropertySource("application.properties")
public class BotConfig {

    @Value("${bot.name}")
    String botName;
    @Value("${bot.token}")
    String token;
    @Value("${bot.owner}")
    Long ownerId;

    @Bean
    public TelegramBotsApi telegramBotsApi() throws TelegramApiException {
        return new TelegramBotsApi(DefaultBotSession.class);
    }

    @Bean
    public TelegramBotServiceImpl telegramBotService(BotLogicService botLogicService,
                                                     AppointmentService appointmentService,
                                                     ServicePriceProvider servicePriceProvider,
                                                     NotificationService notificationService,
                                                     AppointmentRepository appointmentRepository,
                                                     ServiceRepository serviceRepository,
                                                     RegisterService registerService,
                                                     BotConfig config) {
        return new TelegramBotServiceImpl(botLogicService, appointmentService,
                servicePriceProvider, notificationService,
                appointmentRepository,serviceRepository, registerService, config);
    }

    @Bean
    @Lazy
    public TelegramMessageSender telegramMessageSender(@Lazy TelegramBotService telegramBotService) {
        return new TelegramMessageSender(telegramBotService);
    }

    @Bean
    public BotLogicService botLogicService(ChatUserService chatUserService,
                                           MessageSender messageSender,
                                           TelegramMessageSender telegramMessageSender) {
        return new BotLogicServiceImpl(chatUserService, messageSender, telegramMessageSender);
    }

    @Bean
    public AppointmentService appointmentService(AppointmentRepository appointmentRepository,
                                                 CosmetologistRepository cosmetologistRepository,
                                                 ServiceRepository serviceRepository,
                                                 ChatUserRepository chatUserRepository,
                                                 BotLogicService botLogicService,
                                                 NotificationService notificationService) {
        return new AppointmentServiceImpl(appointmentRepository,
                cosmetologistRepository, serviceRepository, chatUserRepository, botLogicService, notificationService);
    }

    @Bean
    public NotificationService notificationService(BotLogicService botLogicService,
                                                   ChatUserRepository chatUserRepository,
                                                   ServicePriceProvider servicePriceProvider) {
        return new NotificationServiceImpl(botLogicService, chatUserRepository, servicePriceProvider);
    }

    @Bean
    public RegisterService registerService(ChatUserRepository chatUserRepository,
                                           TelegramMessageSender telegramMessageSender,
                                           NotificationService notificationService,
                                           CosmetologistRepository cosmetologistRepository,
                                           AppointmentService appointmentService) {
        return new RegisterServiceImpl(chatUserRepository, telegramMessageSender, notificationService, cosmetologistRepository, appointmentService);
    }

    @Bean
    public ChatUserService chatUserService(ChatUserRepository chatUserRepository) {
        return new ChatUserServiceImpl(chatUserRepository);
    }

    @Bean
    public ServicePriceProvider servicePriceProvider(ServiceRepository serviceRepository) {
        return new ServicePriceProviderImpl(serviceRepository);
    }

    @Bean
    public BotInitializer botInitializer(TelegramBotsApi telegramBotsApi,
                                         TelegramBotServiceImpl telegramBotService) {
        return new BotInitializer(telegramBotsApi, telegramBotService);
    }

/*    @Bean
    public AbsSender absSender() {
        return new DefaultAbsSender(new DefaultBotOptions()) {
            @Override
            public String getBotToken() {
                return token;
            }
        };
    }*/
}
