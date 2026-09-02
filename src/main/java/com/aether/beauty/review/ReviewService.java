package com.aether.beauty.review;

import com.aether.beauty.product.Product;
import com.aether.beauty.product.ProductService;
import com.aether.beauty.realtime.RealtimeEventService;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ReviewService {
  private final ReviewRepository reviewRepository;
  private final ProductService productService;
  private final MediaStorageService mediaStorageService;
  private final RealtimeEventService realtimeEventService;

  public ReviewService(
    ReviewRepository reviewRepository,
    ProductService productService,
    MediaStorageService mediaStorageService,
    RealtimeEventService realtimeEventService
  ) {
    this.reviewRepository = reviewRepository;
    this.productService = productService;
    this.mediaStorageService = mediaStorageService;
    this.realtimeEventService = realtimeEventService;
  }

  public List<Review> findByProduct(Long productId) {
    return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
  }

  @Transactional
  public Review submitReview(
    Long productId,
    String sessionId,
    String customerName,
    int rating,
    String comment,
    List<MultipartFile> files
  ) {
    if (customerName == null || customerName.isBlank()) {
      throw new IllegalArgumentException("Please enter your name.");
    }
    if (rating < 1 || rating > 5) {
      throw new IllegalArgumentException("Rating must be between 1 and 5 stars.");
    }
    int mediaCount = files == null ? 0 : (int) files.stream().filter(f -> !f.isEmpty()).count();
    if (mediaCount > mediaStorageService.maxFilesPerReview()) {
      throw new IllegalArgumentException(
        "You can attach up to " + mediaStorageService.maxFilesPerReview() + " photos/videos per review."
      );
    }

    Product product = productService.requireProduct(productId);

    // One review per shopper session per product: resubmitting updates the
    // existing review instead of piling up duplicates from the same person.
    Review review = reviewRepository
      .findByProductIdAndSessionId(productId, sessionId)
      .orElseGet(Review::new);
    boolean isNew = review.getId() == null;

    review.setProduct(product);
    review.setSessionId(sessionId);
    review.setCustomerName(customerName.trim());
    review.setRating(rating);
    review.setComment(comment == null ? null : comment.trim());
    review.setUpdatedAt(Instant.now());
    if (isNew) {
      review.setCreatedAt(Instant.now());
    }

    if (files != null) {
      review.getMedia().clear();
      for (MultipartFile file : files) {
        if (file == null || file.isEmpty()) continue;
        MediaStorageService.StoredMedia stored = mediaStorageService.store(file);
        ReviewMedia media = new ReviewMedia();
        media.setReview(review);
        media.setMediaType(stored.mediaType());
        media.setUrl(stored.url());
        review.getMedia().add(media);
      }
    }

    Review saved = reviewRepository.save(review);
    recomputeProductRating(product);
    realtimeEventService.publish("reviews", productId);
    return saved;
  }

  private void recomputeProductRating(Product product) {
    List<Review> reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(product.getId());
    int count = reviews.size();
    double average = count == 0
      ? 0
      : reviews.stream().mapToInt(Review::getRating).average().orElse(0);
    product.setReviewCount(count);
    product.setAvgRating(Math.round(average * 10.0) / 10.0);
    productService.save(product);
  }
}
