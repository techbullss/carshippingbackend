package io.reflectoring.carshippingbackend.tables;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "commercial_vehicle")
public class CommercialVehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String brand;
    private String model;
    private String type;

    @Column(name = "year_of_manufacture")
    private String yearOfManufacture;

    @Column(name = "condition_type")
    private String conditionType;

    @Column(name = "body_type")
    private String bodyType;

    private String color;

    @Column(name = "engine_type")
    private String engineType;

    @Column(name = "engine_capacity_cc")
    private String engineCapacityCc;

    @Column(name = "fuel_type")
    private String fuelType;

    private String transmission;
    private String seats;
    private String doors;

    @Column(name = "mileage_km")
    private String mileageKm;

    @Column(name = "payload_capacity_kg")
    private String payloadCapacityKg;

    @Column(name = "cargo_volumem3")
    private String cargoVolumeM3;

    @Column(name = "sleeper_capacity")
    private String sleeperCapacity;

    @Column(name = "camper_features")
    private String camperFeatures;

    @Column(name = "price_kes")
    private Double priceKes;

    private String description;
    private String location;

    @Column(name = "owner_type")
    private String ownerType;

    private String features;

    // FIX: Change TEXT to CLOB for Oracle
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String customSpecs; // JSON string

    // FIX: Add proper collection mapping for Oracle
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "commercial_vehicle_images",
            joinColumns = @JoinColumn(name = "vehicle_id")
    )
    @Column(name = "image_url")
    private List<String> imageUrls;
}