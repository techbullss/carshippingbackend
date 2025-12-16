// ItemRequestDTO.java (for creating requests)

package io.reflectoring.carshippingbackend.DTO;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ItemRequestDTO {
    private String clientName;
    private String clientEmail;
    private String clientPhone;
    private String itemName;
    private String category;
    private String description;
    private String originCountry;
    private String destination;
    private Double budget;
    private Integer quantity = 1;
    private String urgency;
    private String notes;
    // Images will be handled as MultipartFile[]
}
