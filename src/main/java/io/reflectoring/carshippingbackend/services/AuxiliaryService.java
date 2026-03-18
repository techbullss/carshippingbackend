package io.reflectoring.carshippingbackend.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import io.reflectoring.carshippingbackend.repository.ItemRequestRepository;
import io.reflectoring.carshippingbackend.repository.ReviewRepository;
import io.reflectoring.carshippingbackend.tables.ItemRequest;
import io.reflectoring.carshippingbackend.tables.Review;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.admin.email:admin@f-carshipping.com}")
    private String adminEmail;

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

        // Handle image uploads
        if (images != null && images.length > 0) {
            List<String> urls = uploadImages(images);
            request.setImageUrls(urls);
        }

        ItemRequest saved = itemRequestRepository.save(request);

        // Send confirmation email
        sendOrderConfirmationEmail(saved);

        return saved;
    }

    // Get client requests with filters
    public Page<ItemRequest> getClientRequestsWithFilters(
            String clientEmail,
            String status,
            String search,
            Pageable pageable) {
        return itemRequestRepository.findClientRequestsWithFilters(clientEmail, status, search, pageable);
    }

    // Get all requests with filters (admin)
    public Page<ItemRequest> getAllRequests(String status, String search, Pageable pageable) {
        return itemRequestRepository.findAllWithFilters(status, search, pageable);
    }

    // Update request status
    public ItemRequest updateRequestStatus(Long id, String status) {
        ItemRequest request = itemRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        request.setStatus(status);
        request.setUpdatedAt(LocalDateTime.now());

        ItemRequest updated = itemRequestRepository.save(request);

        // Send status update email to client
        sendStatusUpdateEmail(updated);

        return updated;
    }

    // Admin update order
    public ItemRequest adminUpdateOrder(Long id, ItemRequest updatedRequest) {
        ItemRequest existingOrder = itemRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Update fields (admin can update everything)
        updateOrderFields(existingOrder, updatedRequest);
        existingOrder.setUpdatedAt(LocalDateTime.now());

        return itemRequestRepository.save(existingOrder);
    }

    // Submit review
    public Review submitReview(Review review, String clientEmail) {
        review.setClientEmail(clientEmail);
        review.setCreatedAt(LocalDateTime.now());
        review.setApproved(true); // Auto-approve
        review.setHelpfulCount(0);
        return reviewRepository.save(review);
    }

    // Get all reviews
    public Page<Review> getAllReviews(Pageable pageable) {
        return reviewRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    // Get reviews for moderation
    public Page<Review> getReviewsForModeration(Boolean approved, Pageable pageable) {
        if (approved != null) {
            return reviewRepository.findByApproved(approved, pageable);
        }
        return reviewRepository.findAll(pageable);
    }

    // Moderate review
    public Review moderateReview(Long id, Boolean approve) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        review.setApproved(approve);
        return reviewRepository.save(review);
    }

    // Delete review
    public void deleteReview(Long id) {
        reviewRepository.deleteById(id);
    }

    // Mark review as helpful
    public Review markAsHelpful(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        review.setHelpfulCount(review.getHelpfulCount() + 1);
        return reviewRepository.save(review);
    }

    // Search reviews
    public Page<Review> searchReviews(String search, Pageable pageable) {
        return reviewRepository.searchReviews(search, pageable);
    }

    // Get review stats
    public Map<String, Object> getReviewStats() {
        Map<String, Object> stats = new HashMap<>();

        Double avgRating = reviewRepository.getAverageRating();
        stats.put("averageRating", avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0);
        stats.put("totalReviews", reviewRepository.count());

        List<Object[]> distribution = reviewRepository.getRatingDistribution();
        Map<Integer, Map<String, Object>> breakdown = new HashMap<>();

        for (int i = 1; i <= 5; i++) {
            Map<String, Object> ratingData = new HashMap<>();
            ratingData.put("count", 0);
            ratingData.put("percentage", 0.0);
            breakdown.put(i, ratingData);
        }

        long total = reviewRepository.count();
        for (Object[] row : distribution) {
            Integer rating = (Integer) row[0];
            Long count = (Long) row[1];
            if (rating >= 1 && rating <= 5) {
                breakdown.get(rating).put("count", count.intValue());
                if (total > 0) {
                    double percentage = (count * 100.0) / total;
                    breakdown.get(rating).put("percentage", Math.round(percentage * 10.0) / 10.0);
                }
            }
        }

        stats.put("ratingBreakdown", breakdown);
        return stats;
    }

    // Get dashboard stats
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalRequests", itemRequestRepository.count());
        stats.put("pendingRequests", itemRequestRepository.countByStatus("PENDING"));
        stats.put("sourcingRequests", itemRequestRepository.countByStatus("SOURCING"));
        stats.put("inTransitRequests", itemRequestRepository.countByStatus("IN_TRANSIT"));
        stats.put("deliveredRequests", itemRequestRepository.countByStatus("DELIVERED"));
        stats.put("cancelledRequests", itemRequestRepository.countByStatus("CANCELLED"));

        Double avgRating = reviewRepository.getAverageRating();
        stats.put("averageRating", avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0);
        stats.put("totalReviews", reviewRepository.count());

        return stats;
    }

    // Get single order by ID
    public ItemRequest getOrderById(Long id, String clientEmail) {
        ItemRequest order = itemRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Check authorization for non-admin users
        if (clientEmail != null && !order.getClientEmail().equals(clientEmail)) {
            throw new RuntimeException("You are not authorized to access this order");
        }

        return order;
    }

    // Update order (client)
    public ItemRequest updateOrder(Long id, ItemRequest updatedRequest, MultipartFile[] images, String clientEmail) throws IOException {
        ItemRequest existingOrder = getOrderById(id, clientEmail);

        // Check if order can be edited
        if (!Arrays.asList("PENDING", "SOURCING").contains(existingOrder.getStatus())) {
            throw new RuntimeException("Order cannot be edited in its current status");
        }

        // Update editable fields
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

        // Update images if provided
        if (images != null && images.length > 0) {
            List<String> newUrls = uploadImages(images);
            if (existingOrder.getImageUrls() != null) {
                existingOrder.getImageUrls().addAll(newUrls);
            } else {
                existingOrder.setImageUrls(newUrls);
            }
        }

        ItemRequest saved = itemRequestRepository.save(existingOrder);

        // Notify admin of update
        sendOrderUpdateNotification(saved);

        return saved;
    }

    // Update only images
    public ItemRequest updateOrderImages(Long id, MultipartFile[] images, String clientEmail) throws IOException {
        ItemRequest existingOrder = getOrderById(id, clientEmail);

        if (!Arrays.asList("PENDING", "SOURCING").contains(existingOrder.getStatus())) {
            throw new RuntimeException("Cannot update images in current status");
        }

        List<String> newUrls = uploadImages(images);
        if (existingOrder.getImageUrls() != null) {
            existingOrder.getImageUrls().addAll(newUrls);
        } else {
            existingOrder.setImageUrls(newUrls);
        }

        existingOrder.setUpdatedAt(LocalDateTime.now());
        return itemRequestRepository.save(existingOrder);
    }

    // Helper: Upload images to Cloudinary
    private List<String> uploadImages(MultipartFile[] images) throws IOException {
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
        return urls;
    }

    // Helper: Update order fields
    private void updateOrderFields(ItemRequest existing, ItemRequest updated) {
        if (updated.getItemName() != null) existing.setItemName(updated.getItemName());
        if (updated.getCategory() != null) existing.setCategory(updated.getCategory());
        if (updated.getDescription() != null) existing.setDescription(updated.getDescription());
        if (updated.getOriginCountry() != null) existing.setOriginCountry(updated.getOriginCountry());
        if (updated.getDestination() != null) existing.setDestination(updated.getDestination());
        if (updated.getBudget() != null) existing.setBudget(updated.getBudget());
        if (updated.getQuantity() != null) existing.setQuantity(updated.getQuantity());
        if (updated.getUrgency() != null) existing.setUrgency(updated.getUrgency());
        if (updated.getNotes() != null) existing.setNotes(updated.getNotes());
        if (updated.getStatus() != null) existing.setStatus(updated.getStatus());
        if (updated.getClientName() != null) existing.setClientName(updated.getClientName());
        if (updated.getClientPhone() != null) existing.setClientPhone(updated.getClientPhone());
    }

    // Helper: Extract public ID from Cloudinary URL
    private String extractPublicId(String url) {
        try {
            Pattern pattern = Pattern.compile("/v\\d+/(.+?)\\.");
            Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                return matcher.group(1);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    // Helper: Send order confirmation email
    private void sendOrderConfirmationEmail(ItemRequest order) {
        if (emailService != null) {
            String subject = "Item Request Submitted Successfully";
            String content = String.format("""
                    Dear %s,
                    
                    Your request for "%s" has been submitted successfully.
                    Request ID: %s
                    
                    Our team will review your request and contact you within 24-48 hours.
                    
                    Best regards,
                    Shipping Team
                    """,
                    order.getClientName(),
                    order.getItemName(),
                    order.getRequestId());

            emailService.sendSimpleMessage(order.getClientEmail(), subject, content);
        }
    }

    // Helper: Send status update email
    private void sendStatusUpdateEmail(ItemRequest order) {
        if (emailService != null) {
            String subject = "Order Status Updated";
            String content = String.format("""
                    Dear %s,
                    
                    Your order status has been updated.
                    
                    Request ID: %s
                    New Status: %s
                    
                    Log in to your dashboard to view details.
                    
                    Best regards,
                    Shipping Team
                    """,
                    order.getClientName(),
                    order.getRequestId(),
                    order.getStatus());

            emailService.sendSimpleMessage(order.getClientEmail(), subject, content);
        }
    }

    // Helper: Send order update notification to admin
    private void sendOrderUpdateNotification(ItemRequest order) {
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
                    
                    Please review the changes.
                    
                    Regards,
                    Shipping System
                    """,
                    order.getClientName(),
                    order.getRequestId(),
                    order.getItemName(),
                    order.getStatus(),
                    order.getUpdatedAt());

            emailService.sendSimpleMessage(adminEmail, subject, content);
        }
    }
}