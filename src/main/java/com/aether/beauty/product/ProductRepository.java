package com.aether.beauty.product;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
  List<Product> findByActiveTrueOrderByNameAsc();

  List<Product> findByCategoryAndActiveTrueOrderByNameAsc(String category);

  Optional<Product> findByNameIgnoreCase(String name);
}
