package com.aether.beauty.api.dto;

import java.math.BigDecimal;

public record CouponValidationResponse(
  String code,
  BigDecimal subtotal,
  BigDecimal discount,
  BigDecimal newSubtotal
) {}
