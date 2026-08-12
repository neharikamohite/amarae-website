package com.aether.beauty.product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ProductSeedData implements CommandLineRunner {
  private final ProductRepository productRepository;

  public ProductSeedData(ProductRepository productRepository) {
    this.productRepository = productRepository;
  }

  @Override
  public void run(String... args) {
    Set<String> launchNames = Set.of(
      "Ocean Mist",
      "Mystic Rose",
      "Amber Veil",
      "Golden Saffron",
      "Velvet Oud",
      "Pear Bloom",
      "Citrus Noir",
      "Vanilla Muse",
      "Jasmine Rain",
      "Midnight Musk"
    );

    productRepository
      .findAll()
      .stream()
      .filter(product -> !launchNames.contains(product.getName()))
      .forEach(product -> {
        product.setActive(false);
        product.setUpdatedAt(Instant.now());
        productRepository.save(product);
      });

    save("Ocean Mist", "fresh", "Italian bergamot, sea salt, driftwood, and clean musk for hot-day freshness.", "2499.00", "assets/Perfume Bottle Falling Under Water _ Artistic Luxury Fragrance Design.webp", 32);
    save("Mystic Rose", "floral", "Velvet rose, lychee, pink pepper, and amber for soft romantic evenings.", "2799.00", "assets/red.jpg", 26);
    save("Amber Veil", "warm", "Vanilla, sandalwood, tonka, and white musk for a warm lasting trail.", "2999.00", "assets/brown.jpg", 28);
    save("Golden Saffron", "luxury", "Saffron, jasmine, cedar, and amberwood with a premium Indian festive mood.", "3499.00", "assets/perfume-bottle-on-golden-satin-fabric-with-dried-w-2023-11-27-05-07-23-utc_1_3486d0f5-6587-4b89-a392-6dffe86c0ea1.webp", 18);
    save("Velvet Oud", "luxury", "Oud, rosewood, smoked vanilla, and leather for confident night wear.", "3999.00", "assets/card2.jpg", 16);
    save("Pear Bloom", "floral", "Pear skin, peony, freesia, and soft musk for a clean feminine signature.", "2399.00", "assets/elegant-purple-floral-perfume-bottle-on-display-png.webp", 34);
    save("Citrus Noir", "fresh", "Mandarin, neroli, vetiver, and tea leaves for sharp everyday polish.", "2599.00", "assets/green.jpg", 30);
    save("Vanilla Muse", "gourmand", "Madagascar vanilla, almond milk, caramel, and cashmere woods.", "2899.00", "assets/sunset.jpg", 24);
    save("Jasmine Rain", "floral", "Jasmine sambac, water lily, dew accord, and creamy sandalwood.", "2699.00", "assets/bottle-aquatic-perfume-on-stones-600nw-2460780335.webp", 29);
    save("Midnight Musk", "warm", "Blackcurrant, incense, amber, and skin musk for a mature evening aura.", "3199.00", "assets/4c399652be8a5188072c6f063875c4cb.jpg", 21);
  }

  private void save(
    String name,
    String category,
    String description,
    String price,
    String imageUrl,
    int stock
  ) {
    Product product = productRepository.findByNameIgnoreCase(name).orElseGet(Product::new);
    product.setName(name);
    product.setCategory(category);
    product.setDescription(description);
    product.setPrice(new BigDecimal(price));
    product.setImageUrl(imageUrl);
    product.setStock(stock);
    product.setActive(true);
    product.setUpdatedAt(Instant.now());
    productRepository.save(product);
  }
}
