package com.aether.beauty.product;

import com.aether.beauty.realtime.RealtimeEventService;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
  private final ProductRepository productRepository;
  private final RealtimeEventService realtimeEventService;

  public ProductService(
    ProductRepository productRepository,
    RealtimeEventService realtimeEventService
  ) {
    this.productRepository = productRepository;
    this.realtimeEventService = realtimeEventService;
  }

  public List<Product> findProducts(String category) {
    if (category == null || category.isBlank() || category.equalsIgnoreCase("all")) {
      return productRepository.findByActiveTrueOrderByNameAsc();
    }
    return productRepository.findByCategoryAndActiveTrueOrderByNameAsc(category);
  }

  public Product requireProduct(Long productId) {
    return productRepository
      .findById(productId)
      .filter(Product::isActive)
      .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));
  }

  @Transactional
  public Product save(Product product) {
    product.setUpdatedAt(Instant.now());
    Product saved = productRepository.save(product);
    realtimeEventService.publish("products", saved.getId());
    return saved;
  }
}
