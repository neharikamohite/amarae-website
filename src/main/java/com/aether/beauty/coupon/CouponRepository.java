package com.aether.beauty.coupon;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
  Optional<Coupon> findByCodeIgnoreCaseAndActiveTrue(String code);

  Optional<Coupon> findByCodeIgnoreCase(String code);
}
