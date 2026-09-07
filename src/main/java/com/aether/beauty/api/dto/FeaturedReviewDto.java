package com.aether.beauty.api.dto;

import java.time.Instant;

public record FeaturedReviewDto(
  Long id,
  String customerName,
  int rating,
  String comment,
  String productName,
  Instant createdAt
) {}
