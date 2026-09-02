package com.aether.beauty.review;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
  List<Review> findByProductIdOrderByCreatedAtDesc(Long productId);

  Optional<Review> findByProductIdAndSessionId(Long productId, String sessionId);

  long countByProductId(Long productId);
}
