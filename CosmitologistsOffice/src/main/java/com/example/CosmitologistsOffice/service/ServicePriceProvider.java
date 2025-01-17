package com.example.CosmitologistsOffice.service;

import java.math.BigDecimal;
import java.util.Map;

public interface ServicePriceProvider {
    BigDecimal getServicePrice(String selectedService);
    Map<String, BigDecimal> getServicePrices();
}
