package com.aether.beauty.api;

import com.aether.beauty.api.dto.CouponValidateRequest;
import com.aether.beauty.api.dto.CouponValidationResponse;
import com.aether.beauty.cart.CartItem;
import com.aether.beauty.cart.CartService;
import com.aether.beauty.coupon.CouponResult;
import com.aether.beauty.coupon.CouponService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Lets the cart show a coupon's discount before checkout. This is a
// preview only — OrderService re-validates the code and recomputes the
// discount itself when the order is actually created, so the amount
// charged is never trusted from the frontend.
@RestController
@RequestMapping("/api/coupons")
public class CouponController {
  private final CouponService couponService;
  private final CartService cartService;

  public CouponController(CouponService couponService, CartService cartService) {
    this.couponService = couponService;
    this.cartService = cartService;
  }

  @PostMapping("/validate")
  public CouponValidationResponse validate(
    @RequestHeader("X-Aether-Session") String sessionId,
    @Valid @RequestBody CouponValidateRequest request
  ) {
    List<CartItem> items = cartService.getCart(sessionId);
    if (items.isEmpty()) {
      throw new IllegalStateException("Your cart is empty.");
    }
    BigDecimal subtotal = items
      .stream()
      .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
      .reduce(BigDecimal.ZERO, BigDecimal::add);

    CouponResult result = couponService.validate(request.code(), subtotal);
    return new CouponValidationResponse(result.code(), subtotal, result.discount(), subtotal.subtract(result.discount()));
  }
}
