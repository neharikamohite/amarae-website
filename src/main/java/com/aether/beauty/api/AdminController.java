package com.aether.beauty.api;

import com.aether.beauty.admin.AdminAuthService;
import com.aether.beauty.api.dto.AdminLoginRequest;
import com.aether.beauty.api.dto.AdminOrderDto;
import com.aether.beauty.api.dto.AdminOrderUpdateRequest;
import com.aether.beauty.order.CustomerOrder;
import com.aether.beauty.order.OrderService;
import com.aether.beauty.order.OrderStatus;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
  private final AdminAuthService adminAuthService;
  private final OrderService orderService;
  private final ApiMapper apiMapper;

  public AdminController(AdminAuthService adminAuthService, OrderService orderService, ApiMapper apiMapper) {
    this.adminAuthService = adminAuthService;
    this.orderService = orderService;
    this.apiMapper = apiMapper;
  }

  @PostMapping("/login")
  public Map<String, String> login(@Valid @RequestBody AdminLoginRequest request) {
    return Map.of("token", adminAuthService.login(request.password()));
  }

  @DeleteMapping("/logout")
  public void logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
    adminAuthService.logout(AuthController.bearerToken(authorization));
  }

  @GetMapping("/orders")
  @Transactional(readOnly = true)
  public List<AdminOrderDto> orders(@RequestHeader(value = "Authorization", required = false) String authorization) {
    adminAuthService.requireAdmin(AuthController.bearerToken(authorization));
    return orderService.allOrdersMostRecentFirst().stream().map(this::toAdminDto).toList();
  }

  @PatchMapping("/orders/{id}")
  @Transactional
  public AdminOrderDto updateOrder(
    @RequestHeader(value = "Authorization", required = false) String authorization,
    @PathVariable Long id,
    @RequestBody AdminOrderUpdateRequest request
  ) {
    adminAuthService.requireAdmin(AuthController.bearerToken(authorization));
    OrderStatus status = null;
    if (request.status() != null && !request.status().isBlank()) {
      try {
        status = OrderStatus.valueOf(request.status().trim().toUpperCase());
      } catch (IllegalArgumentException ex) {
        throw new IllegalArgumentException("\"" + request.status() + "\" isn't a recognized order status.");
      }
    }
    CustomerOrder order = orderService.updateFulfillment(
      id,
      status,
      request.trackingCourier(),
      request.trackingNumber(),
      request.trackingUrl()
    );
    return toAdminDto(order);
  }

  private AdminOrderDto toAdminDto(CustomerOrder order) {
    return new AdminOrderDto(
      apiMapper.toOrderDto(order),
      order.getCustomerName(),
      order.getEmail(),
      order.getPhone(),
      order.getShippingAddressLine(),
      order.getShippingCity(),
      order.getShippingState(),
      order.getShippingPinCode()
    );
  }
}
