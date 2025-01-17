package com.example.CosmitologistsOffice.service.impl;

import com.example.CosmitologistsOffice.service.ServicePriceProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class ServicePriceProviderImpl implements ServicePriceProvider {
    public static Map<String, BigDecimal> servicePrices = new HashMap<>();
    static {
        // Инициализация цен для услуг
        servicePrices.put("Атравматичная чистка", new BigDecimal("2500.00"));
        servicePrices.put("Ультразвуковая чистка", new BigDecimal("2000.00"));
        servicePrices.put("Массаж", new BigDecimal("2500.00"));
        servicePrices.put("Карбокси терапия", new BigDecimal("2500.00"));
        servicePrices.put("Уход по типу кожи", new BigDecimal("2000.00"));
        servicePrices.put("Подбор домашнего ухода", new BigDecimal("2500.00"));
    }
@Override
    public  BigDecimal getServicePrice(String selectedService) {
        // Если услуга найдена, возвращаем ее цену, иначе 0.00
        return servicePrices.getOrDefault(selectedService, BigDecimal.ZERO);
    }

    public Map<String, BigDecimal> getServicePrices() {
        return Collections.unmodifiableMap(servicePrices);
    }
}
