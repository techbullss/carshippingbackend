package io.reflectoring.carshippingbackend.tables;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "cars")
public class Car {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String refNo = "FCar-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

    @Column(nullable = false)
    private String brand;

    private String refLink;

    @Column(nullable = false)
    private String model;

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

    @Column(name = "price_kes")
    private String priceKes;

    // FIX 1: Change LONGTEXT to CLOB for Oracle
    @Lob
    @Column(columnDefinition = "CLOB")
    private String description;

    private String location;
    private String ownerType;
    private String features;
    private String seller;

    // FIX 2: Change LONGTEXT to CLOB for Oracle
    @Lob
    @Column(columnDefinition = "CLOB")
    private String customSpecs;

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