package com.aether.beauty.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CheckoutRequest(
  @NotBlank String sessionId,
  @NotBlank String customerName,
  @Email @NotBlank String email,
  @NotBlank String shippingAddressLine,
  @NotBlank String shippingCity,
  @NotBlank String shippingState,
  @NotBlank String shippingPinCode,
  @NotBlank String phone,
  Long complimentaryProductId
) {}
