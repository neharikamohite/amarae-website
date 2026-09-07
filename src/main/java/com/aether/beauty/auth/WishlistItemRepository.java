package com.aether.beauty.auth;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
  List<WishlistItem> findByUserIdOrderByCreatedAtDesc(Long userId);

  Optional<WishlistItem> findByUserIdAndProductId(Long userId, Long productId);

  void deleteByUserIdAndProductId(Long userId, Long productId);

  boolean existsByUserIdAndProductId(Long userId, Long productId);
}
