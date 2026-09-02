package com.aether.beauty.api.dto;

import java.time.Instant;
import java.util.List;

public record ReviewDto(
  Long id,
  Long productId,
  String customerName,
  int rating,
  String comment,
  Instant createdAt,
  List<ReviewMediaDto> media
) {}
