package io.reflectoring.carshippingbackend.services;

import io.reflectoring.carshippingbackend.DTO.ReviewRequest;
import io.reflectoring.carshippingbackend.repository.ReviewRepositorySeller;
import io.reflectoring.carshippingbackend.tables.Review;
import io.reflectoring.carshippingbackend.tables.ReviewSeller;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepositorySeller reviewRepository;

    public ReviewSeller createReview(ReviewRequest request) {

        if (request.getRating() == null ||
                request.getRating() < 1 ||
                request.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        if (request.getComment() == null ||
                request.getComment().isBlank()) {
            throw new IllegalArgumentException("Comment cannot be empty");
        }

        ReviewSeller review = ReviewSeller.builder()
                .vehicleId(request.getVehicleId())
                .sellerId(request.getSellerId())
                .rating(request.getRating())
                .comment(request.getComment())
                .reviewerName(
                        request.getReviewerName() != null
                                ? request.getReviewerName()
                                : "Anonymous"
                )
                .approved(false) // Always false until admin approves
                .createdAt(LocalDateTime.now())
                .build();

        return reviewRepository.save(review);
    }

    public List<ReviewSeller> getSellerReviews(Long sellerId) {
        return reviewRepository.findBySellerIdAndApprovedTrue(sellerId);
    }

    public List<ReviewSeller> getVehicleReviews(Long vehicleId) {
        return reviewRepository.findByVehicleIdAndApprovedTrue(vehicleId);
    }

    public ReviewSeller approveReview(Long reviewId) {
        ReviewSeller review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        review.setApproved(true);
        return reviewRepository.save(review);
    }
}