package io.reflectoring.carshippingbackend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UploadResponse {
    private boolean success;
    private String message;
    private ImageDTO image;
}
