package com.aether.beauty.shipping;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class ShippingService {
  // Indian PIN codes: exactly 6 digits, first digit never 0.
  private static final Pattern PIN_CODE = Pattern.compile("^[1-9][0-9]{5}$");
  // Indian mobile numbers: 10 digits, starting 6-9. Accepts an optional
  // +91/0 prefix or spaces, which are stripped before matching.
  private static final Pattern MOBILE = Pattern.compile("^[6-9][0-9]{9}$");

  private final ShippingProperties properties;

  public ShippingService(ShippingProperties properties) {
    this.properties = properties;
  }

  public BigDecimal computeShippingFee(BigDecimal subtotal) {
    if (subtotal.compareTo(properties.getFreeThreshold()) >= 0) {
      return BigDecimal.ZERO;
    }
    return properties.getFlatFee();
  }

  public BigDecimal freeShippingThreshold() {
    return properties.getFreeThreshold();
  }

  /**
   * We currently only fulfil orders inside India. There's no separate
   * country field on the checkout form, so a well-formed 6-digit Indian PIN
   * code is the practical gate: it rejects the request before payment
   * rather than accepting an order we can't actually ship.
   */
  public void requireValidIndianAddress(String pinCode, String phone) {
    String normalizedPin = pinCode == null ? "" : pinCode.trim();
    if (!PIN_CODE.matcher(normalizedPin).matches()) {
      throw new IllegalStateException(
        "We currently ship only within India. Please enter a valid 6-digit Indian PIN code."
      );
    }
    String normalizedPhone = phone == null ? "" : phone.replaceAll("[^0-9]", "");
    if (normalizedPhone.startsWith("91") && normalizedPhone.length() == 12) {
      normalizedPhone = normalizedPhone.substring(2);
    }
    if (normalizedPhone.startsWith("0") && normalizedPhone.length() == 11) {
      normalizedPhone = normalizedPhone.substring(1);
    }
    if (!MOBILE.matcher(normalizedPhone).matches()) {
      throw new IllegalStateException("Please enter a valid 10-digit Indian mobile number for delivery updates.");
    }
  }
}
