package com.aether.beauty.api;

import com.aether.beauty.api.dto.CheckoutRequest;
import com.aether.beauty.api.dto.OrderDto;
import com.aether.beauty.order.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
  private final OrderService orderService;
  private final ApiMapper apiMapper;

  public OrderController(OrderService orderService, ApiMapper apiMapper) {
    this.orderService = orderService;
    this.apiMapper = apiMapper;
  }

  @GetMapping
  public List<OrderDto> latestOrders() {
    return orderService.latestOrders().stream().map(apiMapper::toOrderDto).toList();
  }

  @PostMapping("/checkout")
  public OrderDto checkout(@Valid @RequestBody CheckoutRequest request) {
    return apiMapper.toOrderDto(orderService.checkout(request));
  }
}
