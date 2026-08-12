package com.aether.beauty.payment;

public record PaymentSession(
  String provider,
  String providerReference,
  String paymentUrl
) {}
