package com.aether.beauty.api.dto;

import java.math.BigDecimal;

public record CartItemDto(
  Long productId,
  String name,
  String category,
  BigDecimal price,
  String imageUrl,
  int quantity,
  BigDecimal lineTotal,
  int sizeMl
) {}
