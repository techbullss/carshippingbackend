package io.reflectoring.carshippingbackend.DTO;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommercialVehicleResponseDTO {
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
    private String customSpecs;
    private String seller;
    private String status;
    private String ownerEmail;

    private List<String> imageUrls;
}

