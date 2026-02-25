package io.reflectoring.carshippingbackend.controllers;

import io.reflectoring.carshippingbackend.DTO.ReviewRequest;
import io.reflectoring.carshippingbackend.services.ReviewService;
import io.reflectoring.carshippingbackend.tables.ReviewSeller;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@CrossOrigin(origins = {"https://f-carshipping.com", "https://www.f-carshipping.com"})
public class ReviewController {

    private final ReviewService reviewService;
    @PostMapping("/save")
    public ResponseEntity<?> createReview(
            @RequestBody ReviewRequest request) {
        reviewService.createReview(request);
        return ResponseEntity.ok("saved");
    }

    // Get approved seller reviews
    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<List<ReviewSeller>> getSellerReviews(
            @PathVariable Long sellerId) {
        return ResponseEntity.ok(
                reviewService.getSellerReviews(sellerId));
    }

    // Get approved vehicle reviews
    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<ReviewSeller>> getVehicleReviews(
            @PathVariable Long vehicleId) {
        return ResponseEntity.ok(
                reviewService.getVehicleReviews(vehicleId));
    }

    // Admin approves review
    @PutMapping("/approve/{reviewId}")
    public ResponseEntity<ReviewSeller> approveReview(
            @PathVariable Long reviewId) {
        return ResponseEntity.ok(
                reviewService.approveReview(reviewId));
    }
}