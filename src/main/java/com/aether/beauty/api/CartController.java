package com.aether.beauty.api;

import com.aether.beauty.api.dto.AddCartItemRequest;
import com.aether.beauty.api.dto.CartDto;
import com.aether.beauty.api.dto.UpdateCartItemRequest;
import com.aether.beauty.cart.CartService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/cart")
public class CartController {
  private final CartService cartService;
  private final ApiMapper apiMapper;

  public CartController(CartService cartService, ApiMapper apiMapper) {
    this.cartService = cartService;
    this.apiMapper = apiMapper;
  }

  @GetMapping
  public CartDto getCart(@RequestHeader("X-Aether-Session") String sessionId) {
    return apiMapper.toCartDto(sessionId, cartService.getCart(sessionId));
  }

  @PostMapping("/items")
  public CartDto addItem(
    @RequestHeader("X-Aether-Session") String sessionId,
    @Valid @RequestBody AddCartItemRequest request
  ) {
    return apiMapper.toCartDto(
      sessionId,
      cartService.addItem(sessionId, request.productId(), request.quantity())
    );
  }

  @PatchMapping("/items/{productId}")
  public CartDto updateItem(
    @RequestHeader("X-Aether-Session") String sessionId,
    @PathVariable Long productId,
    @Valid @RequestBody UpdateCartItemRequest request
  ) {
    return apiMapper.toCartDto(
      sessionId,
      cartService.updateItem(sessionId, productId, request.quantity())
    );
  }

  @DeleteMapping
  public void clearCart(@RequestHeader("X-Aether-Session") String sessionId) {
    cartService.clearCart(sessionId);
  }
}
