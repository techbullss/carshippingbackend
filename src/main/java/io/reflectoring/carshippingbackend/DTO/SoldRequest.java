package io.reflectoring.carshippingbackend.DTO;

import lombok.Data;

@Data
public class SoldRequest {
    private String buyerName;
    private String buyerEmail;
    private String buyerPhoneNumber;
    private String soldBy;
    private String soldDate;
}