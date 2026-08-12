package com.aether.beauty.api.dto;

import com.aether.beauty.order.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderDto(
  Long id,
  OrderStatus status,
  BigDecimal total,
  String paymentProvider,
  String paymentReference,
  String paymentUrl,
  Instant createdAt,
  List<OrderLineDto> lines
) {}
