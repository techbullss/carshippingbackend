package io.reflectoring.carshippingbackend.DTO;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    private String currentPassword;
    private String newPassword;
}