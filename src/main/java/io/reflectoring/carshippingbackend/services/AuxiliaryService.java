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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        emailService.sendOrderConfirmationEmail(saved);

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

        String oldStatus = request.getStatus();
        request.setStatus(status);
        request.setUpdatedAt(LocalDateTime.now());

        ItemRequest updated = itemRequestRepository.save(request);

        // Send status update email to client
        emailService.sendStatusUpdateEmail(updated);

        // If status changed to DELIVERED, send review request
        if ("DELIVERED".equals(status) && !"DELIVERED".equals(oldStatus)) {
            emailService.sendReviewRequestEmail(updated);
        }

        return updated;
    }

    // Admin update order
    public ItemRequest adminUpdateOrder(Long id, ItemRequest updatedRequest) {
        ItemRequest existingOrder = itemRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Update fields (admin can update everything)
        updateOrderFields(existingOrder, updatedRequest);
        existingOrder.setUpdatedAt(LocalDateTime.now());

        ItemRequest saved = itemRequestRepository.save(existingOrder);

        // Send email notification to client about admin edit
        emailService.sendOrderEditedByAdminEmail(saved);

        return saved;
    }

    // Submit review (from website with authentication)
    public Review submitReview(Review review, String clientEmail) {
        review.setClientEmail(clientEmail);
        review.setCreatedAt(LocalDateTime.now());
        review.setApproved(true); // Auto-approve
        review.setHelpfulCount(0);

        Review saved = reviewRepository.save(review);

        // Send thank you email if we have the order ID
        if (review.getOrderId() != null) {
            itemRequestRepository.findById(review.getOrderId()).ifPresent(order -> {
                emailService.sendReviewThankYouEmail(order, review.getRating());
            });
        }

        return saved;
    }

    // Submit review from email link (with token)
    public Review submitReviewFromEmail(Map<String, Object> reviewData) {
        String token = (String) reviewData.get("token");
        Integer rating = (Integer) reviewData.get("rating");
        String comment = (String) reviewData.get("comment");
        String clientName = (String) reviewData.get("clientName");
        String itemName = (String) reviewData.get("itemName");

        // Find order by token
        List<ItemRequest> allOrders = itemRequestRepository.findAll();
        ItemRequest order = null;

        for (ItemRequest o : allOrders) {
            String expectedToken = generateShortToken(o);
            if (expectedToken.equals(token)) {
                order = o;
                break;
            }
        }

        if (order == null) {
            throw new RuntimeException("Invalid or expired review token. Please request a new review link.");
        }

        // Create review
        Review review = new Review();
        review.setClientName(clientName);
        review.setClientEmail(order.getClientEmail());
        review.setItemName(itemName);
        review.setRating(rating);
        review.setComment(comment);
        review.setOrderId(order.getId());
        review.setApproved(true);
        review.setCreatedAt(LocalDateTime.now());
        review.setHelpfulCount(0);

        Review saved = reviewRepository.save(review);

        // Send thank you email
        emailService.sendReviewThankYouEmail(order, rating);

        return saved;
    }

    // Validate review token with orderId (legacy)
    public boolean validateReviewToken(Long orderId, String token) {
        ItemRequest order = itemRequestRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        String expectedToken = generateShortToken(order);
        return expectedToken.equals(token);
    }

    // Validate review by token only (new)
    public Map<String, Object> validateReviewByToken(String token) {
        // Find order by token
        List<ItemRequest> allOrders = itemRequestRepository.findAll();

        for (ItemRequest order : allOrders) {
            String expectedToken = generateShortToken(order);
            if (expectedToken.equals(token)) {
                return Map.of(
                        "valid", true,
                        "clientName", order.getClientName(),
                        "itemName", order.getItemName(),
                        "clientEmail", order.getClientEmail(),
                        "orderId", order.getId()
                );
            }
        }

        return Map.of(
                "valid", false,
                "message", "Invalid or expired review link. Please request a new review link."
        );
    }

    // Generate short token for review links
    private String generateShortToken(ItemRequest order) {
        String data = order.getId() + order.getClientEmail() + order.getRequestId() + System.currentTimeMillis();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.getBytes(StandardCharsets.UTF_8));
            String fullHash = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            return fullHash.substring(0, 12).toLowerCase();
        } catch (Exception e) {
            return UUID.randomUUID().toString().substring(0, 12);
        }
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

        // Store old order for change tracking
        ItemRequest oldOrder = cloneOrder(existingOrder);

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

        // Track changes for email
        Map<String, String> changes = trackChanges(oldOrder, existingOrder);

        // Update images if provided
        if (images != null && images.length > 0) {
            List<String> newUrls = uploadImages(images);
            if (existingOrder.getImageUrls() != null) {
                existingOrder.getImageUrls().addAll(newUrls);
            } else {
                existingOrder.setImageUrls(newUrls);
            }
            changes.put("images", "Added " + images.length + " new image(s)");
        }

        ItemRequest saved = itemRequestRepository.save(existingOrder);

        // Send email notifications
        if (!changes.isEmpty()) {
            emailService.sendOrderEditedByClientEmail(saved, changes);
        }

        return saved;
    }

    // Cancel order by client
    public ItemRequest clientCancelOrder(Long id, String clientEmail, String cancellationReason) {
        ItemRequest order = getOrderById(id, clientEmail);

        if (!Arrays.asList("PENDING", "SOURCING").contains(order.getStatus())) {
            throw new RuntimeException("Order cannot be cancelled in its current status");
        }

        order.setStatus("CANCELLED");
        order.setCancellationReason(cancellationReason);
        order.setUpdatedAt(LocalDateTime.now());

        ItemRequest updated = itemRequestRepository.save(order);

        // Send cancellation email notifications
        emailService.sendOrderCancelledByClientEmail(updated);

        return updated;
    }

    // Cancel order by admin
    public ItemRequest adminCancelOrder(Long id, String cancellationReason) {
        ItemRequest order = itemRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus("CANCELLED");
        order.setCancellationReason(cancellationReason);
        order.setUpdatedAt(LocalDateTime.now());

        ItemRequest updated = itemRequestRepository.save(order);

        // Send cancellation email to client
        emailService.sendOrderCancelledByAdminEmail(updated);

        return updated;
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

        ItemRequest saved = itemRequestRepository.save(existingOrder);

        // Notify admin of image update
        Map<String, String> changes = new HashMap<>();
        changes.put("images", "Added " + images.length + " new image(s)");
        emailService.sendOrderEditedByClientEmail(saved, changes);

        return saved;
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

    // Helper: Clone order for change tracking
    private ItemRequest cloneOrder(ItemRequest order) {
        ItemRequest clone = new ItemRequest();
        clone.setId(order.getId());
        clone.setRequestId(order.getRequestId());
        clone.setClientName(order.getClientName());
        clone.setClientEmail(order.getClientEmail());
        clone.setItemName(order.getItemName());
        clone.setCategory(order.getCategory());
        clone.setDescription(order.getDescription());
        clone.setOriginCountry(order.getOriginCountry());
        clone.setDestination(order.getDestination());
        clone.setBudget(order.getBudget());
        clone.setQuantity(order.getQuantity());
        clone.setUrgency(order.getUrgency());
        clone.setStatus(order.getStatus());
        clone.setNotes(order.getNotes());
        clone.setImageUrls(order.getImageUrls() != null ? new ArrayList<>(order.getImageUrls()) : null);
        return clone;
    }

    // Helper: Track changes between old and new order
    private Map<String, String> trackChanges(ItemRequest oldOrder, ItemRequest newOrder) {
        Map<String, String> changes = new HashMap<>();

        if (!oldOrder.getItemName().equals(newOrder.getItemName())) {
            changes.put("Item Name", oldOrder.getItemName() + " → " + newOrder.getItemName());
        }
        if (!oldOrder.getCategory().equals(newOrder.getCategory())) {
            changes.put("Category", oldOrder.getCategory() + " → " + newOrder.getCategory());
        }
        if (!oldOrder.getOriginCountry().equals(newOrder.getOriginCountry())) {
            changes.put("Origin", oldOrder.getOriginCountry() + " → " + newOrder.getOriginCountry());
        }
        if (!oldOrder.getDestination().equals(newOrder.getDestination())) {
            changes.put("Destination", oldOrder.getDestination() + " → " + newOrder.getDestination());
        }
        if (!oldOrder.getBudget().equals(newOrder.getBudget())) {
            changes.put("Budget", "$" + oldOrder.getBudget() + " → $" + newOrder.getBudget());
        }
        if (!oldOrder.getQuantity().equals(newOrder.getQuantity())) {
            changes.put("Quantity", oldOrder.getQuantity() + " → " + newOrder.getQuantity());
        }
        if (!oldOrder.getUrgency().equals(newOrder.getUrgency())) {
            changes.put("Urgency", oldOrder.getUrgency() + " → " + newOrder.getUrgency());
        }

        return changes;
    }

    // Helper: Generate review token (legacy)
    private String generateReviewToken(ItemRequest order) {
        String data = order.getId() + order.getClientEmail() + order.getRequestId() + "f-carshipping-secret";
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.getBytes("UTF-8"));
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
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
}