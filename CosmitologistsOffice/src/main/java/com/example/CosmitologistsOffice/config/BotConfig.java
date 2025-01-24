package com.example.CosmitologistsOffice.config;

import com.example.CosmitologistsOffice.repository.AppointmentRepository;
import com.example.CosmitologistsOffice.repository.ChatUserRepository;
import com.example.CosmitologistsOffice.repository.CosmetologistRepository;
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
                                                     CosmetologistRepository cosmetologistRepository,
                                                     RegisterService registerService,
                                                     ChatUserService chatUserService,
                                                     BotConfig config) {
        return new TelegramBotServiceImpl(botLogicService, appointmentService,
                servicePriceProvider, notificationService,
                appointmentRepository,cosmetologistRepository, registerService, chatUserService, config);
    }

    @Bean
    @Lazy
    public MessageSender telegramMessageSender(@Lazy TelegramBotService telegramBotService) {
        return new TelegramMessageSender(telegramBotService);
    }
    @Bean
    public BotLogicService botLogicService(ChatUserService chatUserService,
                                           MessageSender messageSender) {
        return new BotLogicServiceImpl(chatUserService, messageSender);
    }

    @Bean
    public AppointmentService appointmentService(AppointmentRepository appointmentRepository) {
        return new AppointmentServiceImpl(appointmentRepository);
    }

    @Bean
    public NotificationService notificationService(BotLogicService botLogicService,
                                                   ChatUserRepository chatUserRepository,
                                                   ServicePriceProvider servicePriceProvider,
                                                   CosmetologistRepository cosmetologistRepository,
                                                   AppointmentRepository appointmentRepository) {
        return new NotificationServiceImpl(botLogicService, chatUserRepository, servicePriceProvider, cosmetologistRepository, appointmentRepository);
    }

    @Bean
    public RegisterService registerService(
                                           MessageSender telegramMessageSender) {
        return new RegisterServiceImpl(telegramMessageSender);
    }

    @Bean
    public ChatUserService chatUserService(ChatUserRepository chatUserRepository) {
        return new ChatUserServiceImpl(chatUserRepository);
    }

    @Bean
    public ServicePriceProvider servicePriceProvider() {
        return new ServicePriceProviderImpl();
    }



    @Bean
    public BotInitializer botInitializer(TelegramBotsApi telegramBotsApi,
                                         TelegramBotServiceImpl telegramBotService) {
        return new BotInitializer(telegramBotsApi, telegramBotService);
    }
}
