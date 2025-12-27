package io.reflectoring.carshippingbackend.DTO;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MotorcycleResponseDTO {
    private Long id;
    private String brand;
    private String model;
    private String type;
    private Integer engineCapacity;
    private String status;
    private Double price;
    private String location;
    private String owner;
    private String description;
    private String seller;
    private List<String> imageUrls;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> features;
    private Integer year;
}

