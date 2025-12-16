package io.reflectoring.carshippingbackend.controllers;

import io.reflectoring.carshippingbackend.services.AuxiliaryService;
import io.reflectoring.carshippingbackend.tables.ItemRequest;
import io.reflectoring.carshippingbackend.tables.Review;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/auxiliary")
@RequiredArgsConstructor
@CrossOrigin(origins = "https://f-carshipping.com")
public class AuxiliaryController {

    private final AuxiliaryService auxiliaryService;

    // Client submits item request
    @PostMapping("/request-item")
    public ResponseEntity<ItemRequest> requestItem(
            @ModelAttribute ItemRequest request,
            @RequestParam(value = "images", required = false) MultipartFile[] images,
            Authentication authentication) throws IOException {

        String clientEmail = authentication.getName();
        ItemRequest saved = auxiliaryService.createItemRequest(request, images, clientEmail);
        return ResponseEntity.ok(saved);
    }

    // Get client's requests
    @GetMapping("/my-requests")
    public ResponseEntity<Page<ItemRequest>> getMyRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            Authentication authentication) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.fromString(sortDir), sortBy));

        String clientEmail = authentication.getName();
        Page<ItemRequest> requests = auxiliaryService.getClientRequests(clientEmail, pageable);
        return ResponseEntity.ok(requests);
    }

    // Admin: Get all requests
    @GetMapping("/requests")
    public ResponseEntity<Page<ItemRequest>> getAllRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ItemRequest> requests = auxiliaryService.getAllRequests(status, search, pageable);
        return ResponseEntity.ok(requests);
    }

    // Admin: Update request status
    @PatchMapping("/requests/{id}/status")
    public ResponseEntity<ItemRequest> updateRequestStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        ItemRequest updated = auxiliaryService.updateRequestStatus(id, status);
        return ResponseEntity.ok(updated);
    }

    // Submit review (NO APPROVAL NEEDED - posts immediately)
    @PostMapping("/reviews")
    public ResponseEntity<Review> submitReview(@RequestBody Review review) {
        Review saved = auxiliaryService.submitReview(review);
        return ResponseEntity.ok(saved);
    }

    // Get public reviews (ALL reviews - no approval filter)
    @GetMapping("/reviews/public")
    public ResponseEntity<Page<Review>> getPublicReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> reviews = auxiliaryService.getAllReviews(pageable);
        return ResponseEntity.ok(reviews);
    }

    // Get review statistics (for rating breakdown)
    @GetMapping("/reviews/stats")
    public ResponseEntity<Map<String, Object>> getReviewStats() {
        Map<String, Object> stats = auxiliaryService.getReviewStats();
        return ResponseEntity.ok(stats);
    }

    // Mark review as helpful
    @PostMapping("/reviews/{id}/helpful")
    public ResponseEntity<Review> markReviewAsHelpful(@PathVariable Long id) {
        Review updated = auxiliaryService.markAsHelpful(id);
        return ResponseEntity.ok(updated);
    }

    // Admin: Get all reviews for moderation (optional - with approval filter)
    @GetMapping("/reviews")
    public ResponseEntity<Page<Review>> getAllReviews(
            @RequestParam(required = false) Boolean approved,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Review> reviews = auxiliaryService.getReviewsForModeration(approved, pageable);
        return ResponseEntity.ok(reviews);
    }

    // Admin: Search reviews
    @GetMapping("/reviews/search")
    public ResponseEntity<Page<Review>> searchReviews(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Review> reviews = auxiliaryService.searchReviews(query, pageable);
        return ResponseEntity.ok(reviews);
    }

    // Admin: Moderate review (optional - keep if you want override capability)
    @PatchMapping("/reviews/{id}/moderate")
    public ResponseEntity<Review> moderateReview(
            @PathVariable Long id,
            @RequestParam Boolean approve) {

        Review updated = auxiliaryService.moderateReview(id, approve);
        return ResponseEntity.ok(updated);
    }

    // Get dashboard stats
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = auxiliaryService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }

    // Helper endpoint for frontend to get item categories
    @GetMapping("/categories")
    public ResponseEntity<String[]> getCategories() {
        String[] categories = {
                "Electronics",
                "Clothing & Fashion",
                "Home & Kitchen",
                "Automotive Parts",
                "Books & Media",
                "Medical Equipment",
                "Sports Equipment",
                "Industrial Tools",
                "Other"
        };
        return ResponseEntity.ok(categories);
    }
}