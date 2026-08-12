package com.aether.beauty.api;

import com.aether.beauty.api.dto.CartDto;
import com.aether.beauty.api.dto.CartItemDto;
import com.aether.beauty.api.dto.OrderDto;
import com.aether.beauty.api.dto.OrderLineDto;
import com.aether.beauty.api.dto.ProductDto;
import com.aether.beauty.cart.CartItem;
import com.aether.beauty.order.CustomerOrder;
import com.aether.beauty.product.Product;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ApiMapper {
  public ProductDto toProductDto(Product product) {
    return new ProductDto(
      product.getId(),
      product.getName(),
      product.getCategory(),
      product.getDescription(),
      product.getPrice(),
      product.getImageUrl(),
      product.getStock()
    );
  }

  public CartDto toCartDto(String sessionId, List<CartItem> items) {
    List<CartItemDto> itemDtos = items.stream().map(this::toCartItemDto).toList();
    BigDecimal total = itemDtos
      .stream()
      .map(CartItemDto::lineTotal)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    return new CartDto(sessionId, itemDtos, total);
  }

  public CartItemDto toCartItemDto(CartItem item) {
    Product product = item.getProduct();
    BigDecimal lineTotal = product
      .getPrice()
      .multiply(BigDecimal.valueOf(item.getQuantity()));
    return new CartItemDto(
      product.getId(),
      product.getName(),
      product.getCategory(),
      product.getPrice(),
      product.getImageUrl(),
      item.getQuantity(),
      lineTotal
    );
  }

  public OrderDto toOrderDto(CustomerOrder order) {
    List<OrderLineDto> lines = order
      .getLines()
      .stream()
      .map(line -> new OrderLineDto(line.getProductName(), line.getUnitPrice(), line.getQuantity()))
      .toList();
    return new OrderDto(
      order.getId(),
      order.getStatus(),
      order.getTotal(),
      order.getPaymentProvider(),
      order.getPaymentReference(),
      order.getPaymentUrl(),
      order.getCreatedAt(),
      lines
    );
  }
}
