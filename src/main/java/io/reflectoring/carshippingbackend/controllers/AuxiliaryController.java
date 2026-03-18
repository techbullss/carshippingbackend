package io.reflectoring.carshippingbackend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

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

    // Get client's requests with search and filter
    @GetMapping("/my-requests")
    public ResponseEntity<Page<ItemRequest>> getMyRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            Authentication authentication) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.fromString(sortDir), sortBy));

        String clientEmail = authentication.getName();
        Page<ItemRequest> requests = auxiliaryService.getClientRequestsWithFilters(clientEmail, status, search, pageable);
        return ResponseEntity.ok(requests);
    }

    // Admin: Get all requests with filters
    @GetMapping("/requests")
    public ResponseEntity<Page<ItemRequest>> getAllRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.fromString(sortDir), sortBy));

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

    // Admin: Edit any order
    @PutMapping("/admin/requests/{id}")
    public ResponseEntity<ItemRequest> adminUpdateOrder(
            @PathVariable Long id,
            @RequestBody ItemRequest updatedRequest) throws IOException {

        ItemRequest updated = auxiliaryService.adminUpdateOrder(id, updatedRequest);
        return ResponseEntity.ok(updated);
    }

    // Admin: Cancel any order with reason (UPDATED)
    @PatchMapping("/admin/requests/{id}/cancel")
    public ResponseEntity<ItemRequest> adminCancelOrder(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {

        ItemRequest updated = auxiliaryService.adminCancelOrder(id, reason);
        return ResponseEntity.ok(updated);
    }

    // Client: Cancel own order with reason (NEW)
    @PatchMapping("/requests/{id}/cancel")
    public ResponseEntity<ItemRequest> clientCancelOrder(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            Authentication authentication) {

        String clientEmail = authentication.getName();
        ItemRequest updated = auxiliaryService.clientCancelOrder(id, clientEmail, reason);
        return ResponseEntity.ok(updated);
    }

    // Submit review (from website)
    @PostMapping("/reviews")
    public ResponseEntity<Review> submitReview(@RequestBody Review review, Authentication authentication) {
        String clientEmail = authentication != null ? authentication.getName() : null;
        Review saved = auxiliaryService.submitReview(review, clientEmail);
        return ResponseEntity.ok(saved);
    }

    // Submit review from email link (NO AUTHENTICATION REQUIRED - NEW)
    @PostMapping("/reviews/from-email")
    public ResponseEntity<Review> submitReviewFromEmail(@RequestBody Map<String, Object> reviewData) {
        Review saved = auxiliaryService.submitReviewFromEmail(reviewData);
        return ResponseEntity.ok(saved);
    }

    // Validate review token (for email links - NEW)
    @GetMapping("/reviews/validate")
    public ResponseEntity<Map<String, Object>> validateReviewToken(
            @RequestParam Long orderId,
            @RequestParam String token) {

        boolean isValid = auxiliaryService.validateReviewToken(orderId, token);

        if (isValid) {
            ItemRequest order = auxiliaryService.getOrderById(orderId, null);
            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "clientName", order.getClientName(),
                    "itemName", order.getItemName(),
                    "clientEmail", order.getClientEmail(),
                    "orderId", order.getId()
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "valid", false,
                    "message", "Invalid or expired review link"
            ));
        }
    }

    // Get public reviews
    @GetMapping("/reviews/public")
    public ResponseEntity<Page<Review>> getPublicReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> reviews = auxiliaryService.getAllReviews(pageable);
        return ResponseEntity.ok(reviews);
    }

    // Get review statistics
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

    // Admin: Get all reviews for moderation
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

    // Admin: Moderate review
    @PatchMapping("/reviews/{id}/moderate")
    public ResponseEntity<Review> moderateReview(
            @PathVariable Long id,
            @RequestParam Boolean approve) {

        Review updated = auxiliaryService.moderateReview(id, approve);
        return ResponseEntity.ok(updated);
    }

    // Admin: Delete review
    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        auxiliaryService.deleteReview(id);
        return ResponseEntity.ok().build();
    }

    // Get dashboard stats
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = auxiliaryService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }

    // Get categories
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

    // Get single order (client)
    @GetMapping("/requests/{id}")
    public ResponseEntity<ItemRequest> getOrderById(
            @PathVariable("id") Long id,
            Authentication authentication) {

        String clientEmail = authentication != null ? authentication.getName() : null;
        ItemRequest order = auxiliaryService.getOrderById(id, clientEmail);
        return ResponseEntity.ok(order);
    }

    // Update order (client edit)
    @PutMapping("/requests/{id}")
    public ResponseEntity<ItemRequest> updateOrder(
            @PathVariable Long id,
            @RequestBody ItemRequest updatedRequest,
            Authentication authentication) throws IOException {

        String clientEmail = authentication.getName();
        ItemRequest updated = auxiliaryService.updateOrder(id, updatedRequest, null, clientEmail);
        return ResponseEntity.ok(updated);
    }

    // Update order images (separate endpoint)
    @PutMapping("/requests/{id}/images")
    public ResponseEntity<ItemRequest> updateOrderImages(
            @PathVariable Long id,
            @RequestParam("images") MultipartFile[] images,
            Authentication authentication) throws IOException {

        String clientEmail = authentication.getName();
        ItemRequest updated = auxiliaryService.updateOrderImages(id, images, clientEmail);
        return ResponseEntity.ok(updated);
    }

    // Test endpoint to send review email (for development - NEW)
    @PostMapping("/test/send-review-email/{orderId}")
    public ResponseEntity<String> sendTestReviewEmail(@PathVariable Long orderId) {
        try {
            ItemRequest order = auxiliaryService.getOrderById(orderId, null);

            return ResponseEntity.ok("Test review email sent to " + order.getClientEmail());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed: " + e.getMessage());
        }
    }
}