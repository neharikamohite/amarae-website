package com.aether.beauty.api.dto;

// All fields optional — the admin dashboard sends only what changed.
// status is the enum name as a string (e.g. "SHIPPED") rather than the
// enum type itself, so an unrecognized value fails with a clear message
// instead of a raw 400 from Jackson.
public record AdminOrderUpdateRequest(
  String status,
  String trackingCourier,
  String trackingNumber,
  String trackingUrl
) {}
