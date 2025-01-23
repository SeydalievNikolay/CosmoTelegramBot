package com.example.CosmitologistsOffice.service.impl;

import com.example.CosmitologistsOffice.service.ServicePriceProvider;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Этот класс предоставляет возможность получения цены для различных услуг,
 * таких как атравматичная чистка, ультразвуковая чистка и другие.
 */
public class ServicePriceProviderImpl implements ServicePriceProvider {
    // Карта, хранящая цены на услуги
    public static Map<String, BigDecimal> servicePrices = new HashMap<>();
    // Статический блок инициализации цен на услуги
    static {
        servicePrices.put("Атравматичная чистка", new BigDecimal("2500.00"));
        servicePrices.put("Ультразвуковая чистка", new BigDecimal("2000.00"));
        servicePrices.put("Массаж", new BigDecimal("2500.00"));
        servicePrices.put("Карбокси терапия", new BigDecimal("2500.00"));
        servicePrices.put("Уход по типу кожи", new BigDecimal("2000.00"));
        servicePrices.put("Подбор домашнего ухода", new BigDecimal("2500.00"));
    }

    /**
     * Получает цену для выбранной услуги.
     * Если услуга не найдена в списке, возвращается нулевая цена.
     *
     * @param selectedService Название услуги, для которой требуется получить цену.
     * @return Цена услуги в виде объекта {@link BigDecimal}.
     */
    @Override
    public BigDecimal getServicePrice(String selectedService) {
        return servicePrices.getOrDefault(selectedService, BigDecimal.ZERO);
    }

    /**
     * Получает все доступные услуги с их ценами.
     *
     * @return Невозможная для изменения карта с услугами и их ценами.
     */
    @Override
    public Map<String, BigDecimal> getServicePrices() {
        return Collections.unmodifiableMap(servicePrices);
    }
}
