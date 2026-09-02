package com.aether.beauty.api;

import com.aether.beauty.api.dto.ReviewDto;
import com.aether.beauty.review.ReviewService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
public class ReviewController {
  private final ReviewService reviewService;
  private final ApiMapper apiMapper;

  public ReviewController(ReviewService reviewService, ApiMapper apiMapper) {
    this.reviewService = reviewService;
    this.apiMapper = apiMapper;
  }

  @GetMapping
  public List<ReviewDto> reviews(@PathVariable Long productId) {
    return reviewService.findByProduct(productId).stream().map(apiMapper::toReviewDto).toList();
  }

  // multipart/form-data so a review can carry photos/videos alongside the
  // star rating, name, and comment in a single request.
  @PostMapping
  public ReviewDto submitReview(
    @PathVariable Long productId,
    @RequestHeader("X-Aether-Session") String sessionId,
    @RequestParam String customerName,
    @RequestParam int rating,
    @RequestParam(required = false) String comment,
    @RequestParam(required = false) List<MultipartFile> files
  ) {
    return apiMapper.toReviewDto(
      reviewService.submitReview(productId, sessionId, customerName, rating, comment, files)
    );
  }
}
