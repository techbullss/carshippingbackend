package io.reflectoring.carshippingbackend.tables;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "motorcycle")  // Explicit table name
public class Motorcycle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String brand;
    private String model;
    private String type;

    @Column(name = "engine_capacity")
    private Integer engineCapacity;
    private String seller;
    private String status;
    private Double price;
    private String location;
    private String owner;
    private Integer year;

    // FIX: Proper collection mapping for features
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "motorcycle_features",
            joinColumns = @JoinColumn(name = "motorcycle_id")
    )
    @Column(name = "feature")
    private List<String> features;

    // FIX: Proper collection mapping for images + change TEXT to VARCHAR2
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "motorcycle_images",
            joinColumns = @JoinColumn(name = "motorcycle_id")
    )
    @Column(name = "image_url", length = 1000)  // Use length instead of TEXT
    private List<String> imageUrls;

    // FIX: Change TEXT to CLOB for Oracle
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}