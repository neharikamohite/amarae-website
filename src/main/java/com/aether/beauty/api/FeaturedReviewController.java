package com.aether.beauty.api;

import com.aether.beauty.api.dto.FeaturedReviewDto;
import com.aether.beauty.review.Review;
import com.aether.beauty.review.ReviewService;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
public class FeaturedReviewController {
  private final ReviewService reviewService;

  public FeaturedReviewController(ReviewService reviewService) {
    this.reviewService = reviewService;
  }

  @GetMapping("/featured")
  @Transactional(readOnly = true)
  public List<FeaturedReviewDto> featured() {
    return reviewService.findFeatured().stream().map(this::toDto).toList();
  }

  private FeaturedReviewDto toDto(Review review) {
    return new FeaturedReviewDto(
      review.getId(),
      review.getCustomerName(),
      review.getRating(),
      review.getComment(),
      review.getProduct().getName(),
      review.getCreatedAt()
    );
  }
}
