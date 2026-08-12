package com.aether.beauty.api;

import com.aether.beauty.api.dto.ProductDto;
import com.aether.beauty.product.ProductService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {
  private final ProductService productService;
  private final ApiMapper apiMapper;

  public ProductController(ProductService productService, ApiMapper apiMapper) {
    this.productService = productService;
    this.apiMapper = apiMapper;
  }

  @GetMapping
  public List<ProductDto> products(@RequestParam(required = false) String category) {
    return productService.findProducts(category).stream().map(apiMapper::toProductDto).toList();
  }
}
