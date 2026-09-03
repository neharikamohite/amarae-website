package com.aether.beauty.api.dto;

// Wraps the customer-facing OrderDto with the contact/shipping details the
// admin dashboard needs but a customer's own order history doesn't.
public record AdminOrderDto(
  OrderDto order,
  String customerName,
  String email,
  String phone,
  String shippingAddressLine,
  String shippingCity,
  String shippingState,
  String shippingPinCode
) {}
