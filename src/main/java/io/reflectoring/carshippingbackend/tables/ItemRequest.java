// ItemRequest.java
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

    @Column(nullable = false)
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

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}
