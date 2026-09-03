package com.aether.beauty.coupon;

import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * There is no admin screen yet for creating coupons (that's part of the
 * upcoming admin dashboard), so this seeds one starter code on every
 * startup — safe to run repeatedly, it only creates the row if it's
 * missing and never overwrites a code you've since edited by hand.
 *
 * To add more codes for now: use the H2 console (see
 * spring.h2.console.enabled in application.properties) and insert into
 * the COUPON table directly, or ask to have more seeded here.
 */
@Component
public class CouponSeedData implements CommandLineRunner {
  private final CouponRepository couponRepository;

  public CouponSeedData(CouponRepository couponRepository) {
    this.couponRepository = couponRepository;
  }

  @Override
  public void run(String... args) {
    if (couponRepository.findByCodeIgnoreCase("WELCOME10").isPresent()) {
      return;
    }
    Coupon coupon = new Coupon();
    coupon.setCode("WELCOME10");
    coupon.setType(CouponType.PERCENT);
    coupon.setValue(new BigDecimal("10"));
    coupon.setMinSubtotal(BigDecimal.ZERO);
    coupon.setActive(true);
    couponRepository.save(coupon);
  }
}
