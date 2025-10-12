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
public class Motorcycle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String brand;
    private String model;
    private String type;
    private Integer engineCapacity;
    private String status;
    private Double price;
    private String location;
    private String owner;
    private int year;
    @ElementCollection
    private List<String> features;

    @ElementCollection
    @Column(name = "image_url", columnDefinition = "TEXT")
    private List<String> imageUrls;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDateTime createdAt;
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
