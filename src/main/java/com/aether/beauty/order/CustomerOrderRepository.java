package com.aether.beauty.order;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
  List<CustomerOrder> findTop25ByOrderByCreatedAtDesc();

  List<CustomerOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

  List<CustomerOrder> findAllByOrderByCreatedAtDesc();

  // Powers the guest "track my order" lookup — requires both the order id
  // AND a matching email before revealing anything, so someone can't just
  // guess sequential order numbers to see a stranger's order.
  Optional<CustomerOrder> findByIdAndEmailIgnoreCase(Long id, String email);
}
