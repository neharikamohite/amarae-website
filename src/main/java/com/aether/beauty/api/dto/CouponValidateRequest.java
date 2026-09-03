package com.aether.beauty.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CouponValidateRequest(@NotBlank String code) {}
