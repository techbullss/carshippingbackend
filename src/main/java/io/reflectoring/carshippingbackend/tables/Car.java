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
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "car_seq")
    @SequenceGenerator(name = "car_seq", sequenceName = "car_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;

    @Column(name = "year_of_manufacture")
    private Integer yearOfManufacture;

    @Column(name = "condition_type")
    private String conditionType;

    @Column(name = "body_type")
    private String bodyType;

    private String color;

    @Column(name = "engine_type")
    private String engineType;

    @Column(name = "engine_capacity_cc")
    private Integer engineCapacityCc;

    @Column(name = "fuel_type")
    private String fuelType;

    private String transmission;
    private Integer seats;
    private Integer doors;

    @Column(name = "mileage_km")
    private Integer mileageKm;

    @Column(name = "price_kes", nullable = false)
    private Long priceKes;

    @Column(columnDefinition = "CLOB")
    private String description;

    private String location;

    @Column(name = "high_breed")
    private Boolean highBreed;

    @Column(name = "trim_level")
    private String trimLevel;

    private String horsepower;
    private String torque;
    private String acceleration;

    @Column(name = "top_speed")
    private String topSpeed;

    @Column(name = "drive_type")
    private String driveType;

    @Column(name = "infotainment_system")
    private String infotainmentSystem;

    @Column(name = "sound_system")
    private String soundSystem;

    @Column(name = "fuel_efficiency")
    private String fuelEfficiency;

    private String warranty;

    @Column(name = "service_history")
    private String serviceHistory;

    @Column(name = "safety_features")
    private String safetyFeatures;

    @Column(name = "luxury_features")
    private String luxuryFeatures;

    @Column(name = "exterior_features")
    private String exteriorFeatures;

    @Column(name = "interior_features")
    private String interiorFeatures;

    // Image URLs mapped to a separate table with FK to cars
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "car_image_urls",
            joinColumns = @JoinColumn(name = "car_id", referencedColumnName = "id",
                    foreignKey = @ForeignKey(name = "fk_car_image_urls_car"))
    )
    @Column(name = "url", nullable = false)
    private List<String> imageUrls = new ArrayList<>();
}
