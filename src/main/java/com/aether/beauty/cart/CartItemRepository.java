package com.aether.beauty.cart;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
  List<CartItem> findBySessionIdOrderByUpdatedAtDesc(String sessionId);

  Optional<CartItem> findBySessionIdAndProductId(String sessionId, Long productId);

  void deleteBySessionId(String sessionId);
}
