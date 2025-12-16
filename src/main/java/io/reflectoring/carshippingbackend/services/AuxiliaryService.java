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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    // Submit review - NO APPROVAL NEEDED
    public Review submitReview(Review review) {
        review.setCreatedAt(LocalDateTime.now());
        // No approval needed - reviews appear immediately
        return reviewRepository.save(review);
    }

    // Get ALL reviews for public display (no approval filter)
    public Page<Review> getAllReviews(Pageable pageable) {
        return reviewRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    // Get reviews for admin view (with optional approval filter)
    public Page<Review> getReviewsForModeration(Boolean approved, Pageable pageable) {
        if (approved != null) {
            return reviewRepository.findByApproved(approved, pageable);
        }
        return reviewRepository.findAll(pageable);
    }

    // Admin: approve/reject review (optional - keep if you want moderation capability)
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
        stats.put("deliveredRequests", itemRequestRepository.findByStatus("DELIVERED", Pageable.unpaged()).getTotalElements());

        // Average rating
        Double avgRating = reviewRepository.getAverageRating();
        stats.put("averageRating", avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0);
        stats.put("totalReviews", reviewRepository.count());

        return stats;
    }

    // NEW METHOD: Get review statistics with rating breakdown
    public Map<String, Object> getReviewStats() {
        Map<String, Object> stats = new HashMap<>();

        // Average rating (all reviews)
        Double avgRating = reviewRepository.getAverageRating();
        stats.put("averageRating", avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0);

        // Total reviews
        stats.put("totalReviews", reviewRepository.count());

        // Get rating distribution
        List<Object[]> distribution = reviewRepository.getRatingDistribution();
        Map<Integer, Map<String, Object>> breakdown = new HashMap<>();

        // Initialize all ratings 1-5
        for (int i = 1; i <= 5; i++) {
            breakdown.put(i, new HashMap<>() {{
                put("count", 0);
                put("percentage", 0.0);
            }});
        }

        // Fill with actual data
        for (Object[] row : distribution) {
            Integer rating = (Integer) row[0];
            Long count = (Long) row[1];
            if (rating >= 1 && rating <= 5) {
                breakdown.get(rating).put("count", count.intValue());
            }
        }

        // Calculate percentages
        long total = reviewRepository.count();
        if (total > 0) {
            for (int i = 1; i <= 5; i++) {
                int count = (int) breakdown.get(i).get("count");
                double percentage = (count * 100.0) / total;
                breakdown.get(i).put("percentage", Math.round(percentage * 10.0) / 10.0);
            }
        }

        stats.put("ratingBreakdown", breakdown);

        return stats;
    }

    // NEW METHOD: Get all reviews (for admin)
    public Page<Review> getAllReviewsForAdmin(Pageable pageable) {
        return reviewRepository.findAll(pageable);
    }

    // NEW METHOD: Search reviews
    public Page<Review> searchReviews(String search, Pageable pageable) {
        return reviewRepository.searchReviews(search, pageable);
    }

    // NEW METHOD: Mark review as helpful
    public Review markAsHelpful(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        review.setHelpfulCount(review.getHelpfulCount() + 1);
        return reviewRepository.save(review);
    }
    // Get single order by ID
    public ItemRequest getOrderById(Long id, String clientEmail) {
        ItemRequest order = itemRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Check if order belongs to client (for client access)
        if (clientEmail != null && !order.getClientEmail().equals(clientEmail)) {
            throw new RuntimeException("You are not authorized to access this order");
        }

        return order;
    }

    // Update order (for client edits)
    public ItemRequest updateOrder(Long id, ItemRequest updatedRequest, MultipartFile[] images, String clientEmail) throws IOException {
        ItemRequest existingOrder = getOrderById(id, clientEmail);

        // Check if order can be edited
        if (!Arrays.asList("PENDING", "SOURCING").contains(existingOrder.getStatus())) {
            throw new RuntimeException("Order cannot be edited as it has already progressed beyond the editing stage");
        }

        // Update fields
        existingOrder.setItemName(updatedRequest.getItemName());
        existingOrder.setCategory(updatedRequest.getCategory());
        existingOrder.setDescription(updatedRequest.getDescription());
        existingOrder.setOriginCountry(updatedRequest.getOriginCountry());
        existingOrder.setDestination(updatedRequest.getDestination());
        existingOrder.setBudget(updatedRequest.getBudget());
        existingOrder.setQuantity(updatedRequest.getQuantity());
        existingOrder.setUrgency(updatedRequest.getUrgency());
        existingOrder.setNotes(updatedRequest.getNotes());
        existingOrder.setUpdatedAt(LocalDateTime.now());

        // Handle images (keep existing + add new)
        List<String> updatedUrls = new ArrayList<>();

        // Keep existing images that are still in the list
        if (updatedRequest.getImageUrls() != null) {
            updatedUrls.addAll(updatedRequest.getImageUrls());
        }

        // Delete removed images from Cloudinary
        if (existingOrder.getImageUrls() != null) {
            List<String> removedUrls = existingOrder.getImageUrls().stream()
                    .filter(url -> updatedRequest.getImageUrls() == null || !updatedRequest.getImageUrls().contains(url))
                    .collect(Collectors.toList());

            for (String url : removedUrls) {
                try {
                    String publicId = extractPublicId(url);
                    if (publicId != null) {
                        cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
                    }
                } catch (Exception ex) {
                    System.err.println("⚠️ Failed to delete Cloudinary image: " + ex.getMessage());
                }
            }
        }

        // Upload new images
        if (images != null && images.length > 0) {
            for (MultipartFile file : images) {
                if (!file.isEmpty()) {
                    Map uploadResult = cloudinary.uploader().upload(
                            file.getBytes(),
                            ObjectUtils.asMap(
                                    "folder", "auxiliary-items",
                                    "resource_type", "auto"
                            )
                    );
                    updatedUrls.add((String) uploadResult.get("secure_url"));
                }
            }
        }

        existingOrder.setImageUrls(updatedUrls);

        // Send email notification to admin
        if (emailService != null) {
            String subject = "Order Updated by Client";
            String content = String.format("""
                Dear Admin,
                
                Client %s has updated their order.
                
                Order Details:
                - Request ID: %s
                - Item: %s
                - Status: %s
                - Updated at: %s
                
                Please review the changes in the admin panel.
                
                Regards,
                Shipping System
                """,
                    existingOrder.getClientName(),
                    existingOrder.getRequestId(),
                    existingOrder.getItemName(),
                    existingOrder.getStatus(),
                    existingOrder.getUpdatedAt());

            // Send to admin email (configure this)
            emailService.sendSimpleMessage("admin@yourdomain.com", subject, content);
        }

        return itemRequestRepository.save(existingOrder);
    }
    /**
     * Simplified version - extracts public ID assuming standard Cloudinary URL format
     */
    private String extractPublicId(String url) {
        try {
            // Example URL: https://res.cloudinary.com/demo/image/upload/v1234567890/auxiliary-items/abc123.jpg

            // Find the version part (starts with /v followed by numbers)
            Pattern pattern = Pattern.compile("/v\\d+/");
            Matcher matcher = pattern.matcher(url);

            if (matcher.find()) {
                int start = matcher.end(); // Position after /v1234567890/
                String afterVersion = url.substring(start);

                // Remove file extension
                int dotIndex = afterVersion.lastIndexOf('.');
                if (dotIndex != -1) {
                    return afterVersion.substring(0, dotIndex);
                }
                return afterVersion;
            }

            // Alternative: Look for the folder name
            int folderIndex = url.indexOf("auxiliary-items/");
            if (folderIndex != -1) {
                String afterFolder = url.substring(folderIndex);
                int dotIndex = afterFolder.lastIndexOf('.');
                if (dotIndex != -1) {
                    return afterFolder.substring(0, dotIndex);
                }
                return afterFolder;
            }

            return null;
        } catch (Exception e) {
            System.err.println("Failed to extract public ID from: " + url);
            return null;
        }
    }
    public void deleteReview(Long id){
        reviewRepository.deleteById(id);
    }
}