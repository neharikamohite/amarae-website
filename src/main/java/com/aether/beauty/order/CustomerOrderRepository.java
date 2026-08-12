package com.aether.beauty.order;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
  List<CustomerOrder> findTop25ByOrderByCreatedAtDesc();
}
