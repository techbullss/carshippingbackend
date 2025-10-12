package io.reflectoring.carshippingbackend.DTO;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class MotorcycleRequestDTO {
    private String brand;
    private String model;
    private String type;
    private Integer engineCapacity;
    private String status;
    private Double price;
    private String location;
    private String owner;
    private String description;
    private int year;
    private List<String> features;

    // NOT populated by JSON — set by controller from @RequestPart("images")
    private List<MultipartFile> images;
}
