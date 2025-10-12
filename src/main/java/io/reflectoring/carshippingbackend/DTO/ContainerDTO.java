package io.reflectoring.carshippingbackend.DTO;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class ContainerDTO {
    private String containerNumber;
    private String size;
    private String type;
    private Double price;
    private String status;
    private List<MultipartFile> images; // uploaded files
}
