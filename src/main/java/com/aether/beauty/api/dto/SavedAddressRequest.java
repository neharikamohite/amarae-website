package com.aether.beauty.api.dto;

import jakarta.validation.constraints.NotBlank;

public record SavedAddressRequest(
  @NotBlank String label,
  @NotBlank String addressLine,
  @NotBlank String city,
  @NotBlank String state,
  @NotBlank String pinCode,
  @NotBlank String phone
) {}
