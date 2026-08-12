package com.aether.beauty.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartDto(String sessionId, List<CartItemDto> items, BigDecimal total) {}
