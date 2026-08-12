package com.aether.beauty.api.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentCallbackRequest(
  @NotBlank String providerReference,
  @NotBlank String status
) {}
