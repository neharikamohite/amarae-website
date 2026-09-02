package com.aether.beauty.api.dto;

import java.math.BigDecimal;

public record ProductDto(
  Long id,
  String name,
  String category,
  String description,
  BigDecimal price,
  String imageUrl,
  int stock,
  int sizeMl,
  double avgRating,
  int reviewCount
) {}
