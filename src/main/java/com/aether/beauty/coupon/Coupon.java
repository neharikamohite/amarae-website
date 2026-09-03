package com.aether.beauty.coupon;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
public class Coupon {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // Always stored upper-cased so lookups are case-insensitive without
  // relying on a database-specific collation.
  @Column(nullable = false, unique = true)
  private String code;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CouponType type;

  // PERCENT: whole-number percent off (10 = 10%). FLAT: rupees off.
  @Column(nullable = false)
  private BigDecimal value = BigDecimal.ZERO;

  // Cart subtotal must reach this amount before the code is accepted.
  @Column(nullable = false)
  private BigDecimal minSubtotal = BigDecimal.ZERO;

  @Column(nullable = false)
  private boolean active = true;

  // Null means it never expires.
  private Instant expiresAt;

  private Instant createdAt = Instant.now();

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public CouponType getType() {
    return type;
  }

  public void setType(CouponType type) {
    this.type = type;
  }

  public BigDecimal getValue() {
    return value;
  }

  public void setValue(BigDecimal value) {
    this.value = value;
  }

  public BigDecimal getMinSubtotal() {
    return minSubtotal;
  }

  public void setMinSubtotal(BigDecimal minSubtotal) {
    this.minSubtotal = minSubtotal;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
