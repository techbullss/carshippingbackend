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
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "car_seq")
    @SequenceGenerator(name = "car_seq", sequenceName = "car_seq", allocationSize = 1)
    private Long id;
    private String refNo = "FCar-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    @Column(nullable = false)
    private String brand;
    private String refLink;

    @Column(nullable = false)
    private String model;

    @Column(name = "year_of_manufacture")
    private String yearOfManufacture; // string to match frontend

    @Column(name = "condition_type")
    private String conditionType;

    @Column(name = "body_type")
    private String bodyType;

    private String color;

    @Column(name = "engine_type")
    private String engineType;

    @Column(name = "engine_capacity_cc")
    private String engineCapacityCc; // string to match frontend

    @Column(name = "fuel_type")
    private String fuelType;

    private String transmission;
    private String seats; // string
    private String doors; // string

    @Column(name = "mileage_km")
    private String mileageKm; // string

    @Column(name = "price_kes")
    private String priceKes; // string

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String description;

    private String location;
    private String ownerType;
    private String features;
    private String seller;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
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
