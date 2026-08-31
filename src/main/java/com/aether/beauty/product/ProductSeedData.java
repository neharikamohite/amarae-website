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
      "Crown Voyage",
      "Wild Sovereign",
      "Royal White Oud",
      "Golden Liberté",
      "Blooming Élise",
      "Crystal Ember"
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

    save("Crown Voyage", "fresh", "Bergamot, green apple, lime, and blackcurrant open into a bold, boundless trail. Amaraè's signature travel-ready scent.", "1499.00", "assets/crown-voyage.jpg", 40);
    save("Wild Sovereign", "woody", "Citrus bergamot and lavender settle into warm amber woods, wild, untamed, and free.", "1499.00", "assets/wild-sovereign.jpg", 35);
    save("Royal White Oud", "luxury", "Saffron and rose petals wrapped around smooth white oud, for an opulent, refined, timeless evening trail.", "1499.00", "assets/royal-white-oud.jpg", 30);
    save("Golden Liberté", "warm", "Orange blossom, lavender, and Madagascar vanilla for a poignant, sensual warmth that lingers.", "1499.00", "assets/golden-liberte.jpg", 32);
    save("Blooming Élise", "floral", "Pink rose and soft petals for a graceful, blooming floral signature.", "1499.00", "assets/blooming-elise.jpg", 38);
    save("Crystal Ember", "luxury", "Saffron threads, white flowers, and warm sandalwood for a radiant, addictive glow.", "1499.00", "assets/crystal-ember.jpg", 28);
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
