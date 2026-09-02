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
      "Crystal Ember",
      "Crown Voyage · 10 ml",
      "Blooming Élise · 10 ml",
      "Double Apple · 10 ml",
      "Grapemint · 10 ml"
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

    save("Crown Voyage", "fresh", "Bergamot, green apple, lime, and blackcurrant open into a bold, boundless trail. Amaraè's signature travel-ready scent.", "1499.00", "assets/crown-voyage.jpg", 40, 100);
    save("Wild Sovereign", "woody", "Citrus bergamot and lavender settle into warm amber woods, wild, untamed, and free.", "1499.00", "assets/wild-sovereign.jpg", 35, 100);
    save("Royal White Oud", "luxury", "Saffron and rose petals wrapped around smooth white oud, for an opulent, refined, timeless evening trail.", "1499.00", "assets/royal-white-oud.jpg", 30, 100);
    save("Golden Liberté", "warm", "Orange blossom, lavender, and Madagascar vanilla for a poignant, sensual warmth that lingers.", "1499.00", "assets/golden-liberte.jpg", 32, 100);
    save("Blooming Élise", "floral", "Pink rose and soft petals for a graceful, blooming floral signature.", "1499.00", "assets/blooming-elise.jpg", 38, 100);
    save("Crystal Ember", "luxury", "Saffron threads, white flowers, and warm sandalwood for a radiant, addictive glow.", "1499.00", "assets/crystal-ember.jpg", 28, 100);

    // Travel-size 10 ml bottles: two are minis of existing 100 ml scents,
    // two (Double Apple, Grapemint) are new fragrances only sold at 10 ml.
    // These are separate purchasable products, not part of the 100 ml
    // launch-offer gift pool.
    save("Crown Voyage · 10 ml", "fresh", "The same bold Crown Voyage trail of bergamot, green apple, lime, and blackcurrant, in a travel-ready 10 ml bottle.", "299.00", "assets/crown-voyage-10ml.jpg", 60, 10);
    save("Blooming Élise · 10 ml", "floral", "The same graceful Blooming Élise pink rose and soft petals, in a travel-ready 10 ml bottle.", "299.00", "assets/blooming-elise-10ml.jpg", 60, 10);
    save("Double Apple · 10 ml", "gourmand", "Crisp red and green apple layered over warm spice and a soft tobacco-leaf base. A juicy, sweet signature in a travel-ready 10 ml bottle.", "299.00", "assets/double-apple-10ml.jpg", 55, 10);
    save("Grapemint · 10 ml", "fresh", "Sun-ripened green grapes brightened with cool mint and a whisper of citrus. A crisp, fruity signature in a travel-ready 10 ml bottle.", "299.00", "assets/grapemint-10ml.jpg", 55, 10);
  }

  private void save(
    String name,
    String category,
    String description,
    String price,
    String imageUrl,
    int stock,
    int sizeMl
  ) {
    Product product = productRepository.findByNameIgnoreCase(name).orElseGet(Product::new);
    product.setName(name);
    product.setCategory(category);
    product.setDescription(description);
    product.setPrice(new BigDecimal(price));
    product.setImageUrl(imageUrl);
    product.setStock(stock);
    product.setSizeMl(sizeMl);
    product.setActive(true);
    product.setUpdatedAt(Instant.now());
    productRepository.save(product);
  }
}
