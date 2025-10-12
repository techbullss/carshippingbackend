package io.reflectoring.carshippingbackend.DTO;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ContainerResponseDTO {
    private Long id;
    private String containerNumber;
    private String size;
    private String type;
    private Double price;
    private String status;
    private List<String> imageUrls;
}
