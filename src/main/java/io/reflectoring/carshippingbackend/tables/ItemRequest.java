package io.reflectoring.carshippingbackend.tables;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "item_requests")
@Data
public class ItemRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String requestId; // Auto-generated like REQ-001

    @Column(nullable = false)
    private String clientName;

    @Column(nullable = false)
    private String clientEmail;

    private String clientPhone;

    @Column(nullable = false)
    private String itemName;

    private String category;

    @Column(length = 1000)
    private String description;

    @ElementCollection
    @CollectionTable(name = "item_images", joinColumns = @JoinColumn(name = "item_request_id"))
    private List<String> imageUrls;

    private String originCountry;
    private String destination;

    private Double budget;
    private Integer quantity = 1;

    private String urgency; // urgent, normal, flexible
    private String status = "PENDING"; // PENDING, SOURCING, IN_TRANSIT, DELIVERED, CANCELLED

    @Column(length = 500)
    private String notes;

    // New field for cancellation reason
    @Column(length = 500)
    private String cancellationReason;

    // New field to track if review request was sent
    private Boolean reviewRequestSent = false;

    // New field to track if review was submitted
    private Boolean reviewSubmitted = false;

    // New field for review token (optional - can be generated on demand)
    @Column(length = 100)
    private String reviewToken;
    @Column(name = "source_type")
    private String sourceType = "AUXILIARY";

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Helper method to check if order can be edited
    public boolean isEditable() {
        return List.of("PENDING", "SOURCING").contains(this.status);
    }

    // Helper method to check if order can be cancelled
    public boolean isCancellable() {
        return List.of("PENDING", "SOURCING", "IN_TRANSIT").contains(this.status);
    }

    // Helper method to check if review can be requested
    public boolean canRequestReview() {
        return "DELIVERED".equals(this.status) && !Boolean.TRUE.equals(this.reviewRequestSent);
    }

    // Helper method to get formatted budget
    public String getFormattedBudget() {
        return budget != null ? String.format("$%.2f", budget) : "Not specified";
    }

    // Helper method to get order summary for emails
    public String getOrderSummary() {
        return String.format("""
                Order Summary:
                Request ID: %s
                Item: %s
                Client: %s
                Status: %s
                Created: %s
                """,
                requestId,
                itemName,
                clientName,
                status,
                createdAt.toLocalDate().toString()
        );
    }
}