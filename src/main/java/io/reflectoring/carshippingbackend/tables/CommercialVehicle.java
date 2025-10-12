package io.reflectoring.carshippingbackend.tables;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommercialVehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String brand;
    private String model;
    private String type;
    private String yearOfManufacture;
    private String conditionType;
    private String bodyType;
    private String color;
    private String engineType;
    private String engineCapacityCc;
    private String fuelType;
    private String transmission;
    private String seats;
    private String doors;
    private String mileageKm;
    private String payloadCapacityKg;
    private String cargoVolumeM3;
    private String sleeperCapacity;
    private String camperFeatures;
    private Double priceKes;
    private String description;
    private String location;
    private String ownerType;
    private String features;

    @Column(columnDefinition = "TEXT")
    private String customSpecs; // JSON string

    @ElementCollection
    private List<String> imageUrls;
}
