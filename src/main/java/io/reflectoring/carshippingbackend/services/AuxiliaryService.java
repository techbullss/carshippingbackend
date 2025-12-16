package io.reflectoring.carshippingbackend.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import io.reflectoring.carshippingbackend.repository.ItemRequestRepository;
import io.reflectoring.carshippingbackend.repository.ReviewRepository;
import io.reflectoring.carshippingbackend.tables.ItemRequest;
import io.reflectoring.carshippingbackend.tables.Review;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuxiliaryService {
    private final ItemRequestRepository itemRequestRepository;
    private final ReviewRepository reviewRepository;
    private final Cloudinary cloudinary;
    private final EmailService emailService;

    // Generate unique request ID
    private String generateRequestId() {
        String prefix = "REQ";
        Long count = itemRequestRepository.count();
        return String.format("%s-%04d", prefix, count + 1);
    }

    // Create new item request
    public ItemRequest createItemRequest(
            ItemRequest request,
            MultipartFile[] images,
            String clientEmail) throws IOException {

        request.setRequestId(generateRequestId());
        request.setClientEmail(clientEmail);
        request.setStatus("PENDING");
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());

        // Handle image uploads to Cloudinary
        if (images != null && images.length > 0) {
            List<String> urls = new ArrayList<>();
            for (MultipartFile file : images) {
                if (!file.isEmpty()) {
                    Map uploadResult = cloudinary.uploader().upload(
                            file.getBytes(),
                            ObjectUtils.asMap(
                                    "folder", "auxiliary-items",
                                    "resource_type", "auto"
                            )
                    );
                    urls.add((String) uploadResult.get("secure_url"));
                }
            }
            request.setImageUrls(urls);
        }

        ItemRequest saved = itemRequestRepository.save(request);

        // Send confirmation email
        if (emailService != null) {
            emailService.sendSimpleMessage(
                    clientEmail,
                    "Item Request Submitted Successfully",
                    String.format("""
                            Dear %s,
                            
                            Your request for "%s" has been submitted successfully.
                            Request ID: %s
                            
                            Our team will review your request and contact you within 24-48 hours.
                            
                            Best regards,
                            Shipping Team
                            """,
                            request.getClientName(),
                            request.getItemName(),
                            saved.getRequestId())
            );
        }

        return saved;
    }

    // Get requests for a client
    public Page<ItemRequest> getClientRequests(String clientEmail, Pageable pageable) {
        return itemRequestRepository.findByClientEmail(clientEmail, pageable);
    }

    // Get all requests (admin)
    public Page<ItemRequest> getAllRequests(String status, String search, Pageable pageable) {
        return itemRequestRepository.searchRequests(status, search, pageable);
    }

    // Update request status
    public ItemRequest updateRequestStatus(Long id, String status) {
        ItemRequest request = itemRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        request.setStatus(status);
        request.setUpdatedAt(LocalDateTime.now());

        return itemRequestRepository.save(request);
    }

    // Submit review
    public Review submitReview(Review review) {
        review.setCreatedAt(LocalDateTime.now());
        review.setApproved(false); // Needs admin approval
        return reviewRepository.save(review);
    }

    // Get approved reviews
    public Page<Review> getApprovedReviews(Pageable pageable) {
        return reviewRepository.findByApprovedTrueOrderByCreatedAtDesc(pageable);
    }

    // Get reviews for moderation (by approval status)
    public Page<Review> getReviewsForModeration(Boolean approved, Pageable pageable) {
        return reviewRepository.findByApproved(approved, pageable);
    }

    // Admin: approve/reject review
    public Review moderateReview(Long id, Boolean approve) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        review.setApproved(approve);
        return reviewRepository.save(review);
    }

    // Get dashboard stats
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // Count by status
        stats.put("totalRequests", itemRequestRepository.count());
        stats.put("pendingRequests", itemRequestRepository.findByStatus("PENDING", Pageable.unpaged()).getTotalElements());
        stats.put("activeShipments", itemRequestRepository.findByStatus("IN_TRANSIT", Pageable.unpaged()).getTotalElements());

        // Average rating
        Double avgRating = reviewRepository.getAverageRating();
        stats.put("averageRating", avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0);
        stats.put("totalReviews", reviewRepository.count());

        return stats;
    }
}