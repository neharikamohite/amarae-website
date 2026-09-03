package com.aether.beauty.coupon;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class CouponService {
  private final CouponRepository couponRepository;

  public CouponService(CouponRepository couponRepository) {
    this.couponRepository = couponRepository;
  }

  /**
   * Validates a coupon code against a cart subtotal and returns the rupee
   * discount it earns. Throws IllegalArgumentException with a
   * customer-facing message on any failure — invalid code, expired,
   * inactive, or subtotal below the coupon's minimum.
   *
   * This is called both from the lightweight preview endpoint (so the cart
   * can show the discount before checkout) and again, authoritatively,
   * inside OrderService.checkout() — the same pattern already used for the
   * launch gift, so a coupon is never trusted from the frontend alone.
   */
  public CouponResult validate(String rawCode, BigDecimal subtotal) {
    if (rawCode == null || rawCode.isBlank()) {
      throw new IllegalArgumentException("Enter a coupon code.");
    }
    String code = rawCode.trim().toUpperCase(Locale.ROOT);
    Coupon coupon = couponRepository
      .findByCodeIgnoreCaseAndActiveTrue(code)
      .orElseThrow(() -> new IllegalArgumentException("That coupon code isn't valid."));

    if (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(Instant.now())) {
      throw new IllegalArgumentException("That coupon code has expired.");
    }
    if (subtotal.compareTo(coupon.getMinSubtotal()) < 0) {
      throw new IllegalArgumentException(
        "This code needs a cart subtotal of at least \u20b9" + coupon.getMinSubtotal().stripTrailingZeros().toPlainString() + "."
      );
    }

    BigDecimal discount = coupon.getType() == CouponType.PERCENT
      ? subtotal.multiply(coupon.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
      : coupon.getValue();

    // Never let a coupon push the order below zero.
    if (discount.compareTo(subtotal) > 0) {
      discount = subtotal;
    }

    return new CouponResult(coupon.getCode(), discount);
  }
}
