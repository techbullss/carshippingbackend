package io.reflectoring.carshippingbackend.DTO;

import lombok.Data;

@Data
public class UpdateRoleRequest {
    private String role;
    private String action; // "ADD" or "REMOVE"
}