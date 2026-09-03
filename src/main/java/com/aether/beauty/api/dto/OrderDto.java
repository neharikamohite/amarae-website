package com.aether.beauty.api.dto;

import com.aether.beauty.order.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderDto(
  Long id,
  OrderStatus status,
  BigDecimal subtotal,
  String couponCode,
  BigDecimal discountAmount,
  BigDecimal shippingFee,
  BigDecimal total,
  String paymentProvider,
  String paymentReference,
  String paymentUrl,
  String trackingCourier,
  String trackingNumber,
  String trackingUrl,
  Instant createdAt,
  List<OrderLineDto> lines
) {}
