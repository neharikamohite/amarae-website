package com.aether.beauty.api.dto;

import java.math.BigDecimal;

public record OrderLineDto(String productName, BigDecimal unitPrice, int quantity, int sizeMl) {}
