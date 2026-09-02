package com.aether.beauty.shipping;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// Tunable without a code change/redeploy — set AETHER_SHIPPING_FLAT_FEE /
// AETHER_SHIPPING_FREE_THRESHOLD as env vars on Render when you finalise
// courier pricing.
@Component
@ConfigurationProperties(prefix = "aether.shipping")
public class ShippingProperties {
  private BigDecimal flatFee = new BigDecimal("79");
  private BigDecimal freeThreshold = new BigDecimal("1499");

  public BigDecimal getFlatFee() {
    return flatFee;
  }

  public void setFlatFee(BigDecimal flatFee) {
    this.flatFee = flatFee;
  }

  public BigDecimal getFreeThreshold() {
    return freeThreshold;
  }

  public void setFreeThreshold(BigDecimal freeThreshold) {
    this.freeThreshold = freeThreshold;
  }
}
