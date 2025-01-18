package com.example.CosmitologistsOffice.service.impl;

import com.example.CosmitologistsOffice.model.Service;
import com.example.CosmitologistsOffice.repository.ServiceRepository;
import com.example.CosmitologistsOffice.service.ServicePriceProvider;

import java.math.BigDecimal;
import java.util.*;

public class ServicePriceProviderImpl implements ServicePriceProvider {
    private final ServiceRepository serviceRepository;
/*
    public static Map<String, BigDecimal> servicePrices = new HashMap<>();

    static {
        // Инициализация цен для услуг
        servicePrices.put("Атравматичная чистка", new BigDecimal("2500.00"));
        servicePrices.put("Ультразвуковая чистка", new BigDecimal("2000.00"));
        servicePrices.put("Массаж", new BigDecimal("2500.00"));
        servicePrices.put("Карбокси терапия", new BigDecimal("2500.00"));
        servicePrices.put("Уход по типу кожи", new BigDecimal("2000.00"));
        servicePrices.put("Подбор домашнего ухода", new BigDecimal("2500.00"));
    }*/

    public ServicePriceProviderImpl(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    @Override
    public BigDecimal getServicePrice(String selectedService) {
        Optional<Service> serviceOptional = serviceRepository.findByName(selectedService);
        return serviceOptional.map(Service::getPrice).orElse(BigDecimal.ZERO);
    }

    public Map<String, BigDecimal> getServicePrices() {
        List<Service> services = serviceRepository.findAll();

        Map<String, BigDecimal> servicePrices = new HashMap<>();
        for (Service service : services) {
            servicePrices.put(service.getName(), service.getPrice());
        }

        return Collections.unmodifiableMap(servicePrices);
    }
}
