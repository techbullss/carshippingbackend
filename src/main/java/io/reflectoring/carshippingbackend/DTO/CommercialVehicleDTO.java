package io.reflectoring.carshippingbackend.DTO;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class CommercialVehicleDTO {
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
    private String status;
    private String seller;


    // For uploads
    private List<MultipartFile> images;
}

