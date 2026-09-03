package com.aether.beauty.api.dto;

public record SavedAddressDto(
  Long id,
  String label,
  String addressLine,
  String city,
  String state,
  String pinCode,
  String phone
) {}
