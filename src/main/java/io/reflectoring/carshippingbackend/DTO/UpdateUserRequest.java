package io.reflectoring.carshippingbackend.DTO;


import lombok.Data;

@Data
public class UpdateUserRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String dateOfBirth;
    private String gender;
    private String streetAddress;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private Boolean newsletter;
    private String shippingFrequency;
    private String vehicleType;
    private String estimatedShippingDate;
}
