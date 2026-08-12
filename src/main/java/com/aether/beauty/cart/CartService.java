package com.aether.beauty.cart;

import com.aether.beauty.product.Product;
import com.aether.beauty.product.ProductService;
import com.aether.beauty.realtime.RealtimeEventService;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {
  private final CartItemRepository cartItemRepository;
  private final ProductService productService;
  private final RealtimeEventService realtimeEventService;

  public CartService(
    CartItemRepository cartItemRepository,
    ProductService productService,
    RealtimeEventService realtimeEventService
  ) {
    this.cartItemRepository = cartItemRepository;
    this.productService = productService;
    this.realtimeEventService = realtimeEventService;
  }

  public List<CartItem> getCart(String sessionId) {
    return cartItemRepository.findBySessionIdOrderByUpdatedAtDesc(sessionId);
  }

  @Transactional
  public List<CartItem> addItem(String sessionId, Long productId, int quantity) {
    Product product = productService.requireProduct(productId);
    CartItem item = cartItemRepository
      .findBySessionIdAndProductId(sessionId, productId)
      .orElseGet(CartItem::new);

    item.setSessionId(sessionId);
    item.setProduct(product);
    item.setQuantity(item.getId() == null ? quantity : item.getQuantity() + quantity);
    item.setUpdatedAt(Instant.now());
    cartItemRepository.save(item);
    realtimeEventService.publish("cart", sessionId);
    return getCart(sessionId);
  }

  @Transactional
  public List<CartItem> updateItem(String sessionId, Long productId, int quantity) {
    cartItemRepository
      .findBySessionIdAndProductId(sessionId, productId)
      .ifPresent(item -> {
        if (quantity <= 0) {
          cartItemRepository.delete(item);
        } else {
          item.setQuantity(quantity);
          item.setUpdatedAt(Instant.now());
          cartItemRepository.save(item);
        }
      });
    realtimeEventService.publish("cart", sessionId);
    return getCart(sessionId);
  }

  @Transactional
  public void clearCart(String sessionId) {
    cartItemRepository.deleteBySessionId(sessionId);
    realtimeEventService.publish("cart", sessionId);
  }
}
