package io.reflectoring.carshippingbackend.tables;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewSeller {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long vehicleId;

    private Long sellerId;

    private Integer rating;

    @Column(length = 1000)
    private String comment;

    private String reviewerName;

    private boolean approved = false;   // Admin must approve

    private LocalDateTime createdAt;
}
