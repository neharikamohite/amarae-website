package com.aether.beauty.api;

import com.aether.beauty.api.dto.CheckoutRequest;
import com.aether.beauty.api.dto.OrderDto;
import com.aether.beauty.auth.AuthService;
import com.aether.beauty.auth.User;
import com.aether.beauty.order.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
  private final OrderService orderService;
  private final AuthService authService;
  private final ApiMapper apiMapper;

  public OrderController(OrderService orderService, AuthService authService, ApiMapper apiMapper) {
    this.orderService = orderService;
    this.authService = authService;
    this.apiMapper = apiMapper;
  }

  // Note: there is deliberately no public "list recent orders" endpoint
  // here — order data (totals, payment references, tracking info) is only
  // ever returned to the customer who placed the order (via
  // /api/account/orders, signed in) or to the admin dashboard
  // (/api/admin/orders, admin-authenticated).
  @PostMapping("/checkout")
  public OrderDto checkout(
    @RequestHeader(value = "Authorization", required = false) String authorization,
    @Valid @RequestBody CheckoutRequest request
  ) {
    // Signing in is optional at checkout — being signed in just links the
    // order to the account afterward so it shows up in order history;
    // guest checkout keeps working exactly as before.
    User user = authService.resolveUserOrNull(AuthController.bearerToken(authorization));
    return apiMapper.toOrderDto(orderService.checkout(request, user));
  }
}
