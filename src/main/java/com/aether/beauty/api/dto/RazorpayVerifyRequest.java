package com.aether.beauty.api.dto;

import jakarta.validation.constraints.NotBlank;

public record RazorpayVerifyRequest(
  @NotBlank String razorpayOrderId,
  @NotBlank String razorpayPaymentId,
  @NotBlank String razorpaySignature
) {}
