package io.reflectoring.carshippingbackend.tables;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "cars",
        indexes = {
                @Index(name = "idx_car_brand", columnList = "brand"),
                @Index(name = "idx_car_price", columnList = "price_kes"),
                @Index(name = "idx_car_year", columnList = "year_of_manufacture"),
                @Index(name = "idx_car_engine_cc", columnList = "engine_capacity_cc"),
                @Index(name = "idx_car_mileage", columnList = "mileage_km")
        })
public class Car {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;

    private Integer yearOfManufacture;
    private String conditionType;
    private String bodyType;
    private String color;
    private String engineType;
    private Integer engineCapacityCc;
    private String fuelType;
    private String transmission;
    private Integer seats;
    private Integer doors;
    private Integer mileageKm;

    @Column(nullable = false)
    private Long priceKes;

    @Column(columnDefinition = "TEXT")
    private String description;
    private String location;
    private Boolean highBreed;
    private String trimLevel;
    private String horsepower;
    private String torque;
    private String acceleration;
    private String topSpeed;
    private String driveType;
    private String infotainmentSystem;
    private String soundSystem;
    private String fuelEfficiency;
    private String warranty;
    private String serviceHistory;
    private String safetyFeatures;
    private String luxuryFeatures;
    private String exteriorFeatures;
    private String interiorFeatures;

    @ElementCollection
    @CollectionTable(name = "car_image_urls", joinColumns = @JoinColumn(name = "car_id"))
    @Column(name = "url")
    private List<String> imageUrls = new ArrayList<>();
}