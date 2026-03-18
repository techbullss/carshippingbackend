package io.reflectoring.carshippingbackend.tables;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Data
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String clientName;

    private String clientEmail;

    @Column(nullable = false)
    private Integer rating; // 1-5

    @Column(length = 2000) // Increased length for detailed reviews
    private String comment;

    @Column(nullable = false)
    private String itemName;

    // NEW: Link to the original order
    private Long orderId;

    private Integer helpfulCount = 0;

    private LocalDateTime createdAt = LocalDateTime.now();

    private Boolean approved = true; // Auto-approve for now

    // Helper method to check if review is from email
    public boolean isFromEmail() {
        return orderId != null;
    }
}