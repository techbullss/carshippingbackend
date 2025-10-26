package io.reflectoring.carshippingbackend.DTO;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private Long id;
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
    private Set<String> preferredCommunication;
    private boolean newsletter;
    private String verificationCode;
    private boolean emailVerified;
    private String shippingFrequency;
    private String vehicleType;
    private String estimatedShippingDate;
    private String sourceCountry;
    private String destinationCountry;
    private Set<String> roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String status;
    private String passportPhoto;
    private String govtId;
    private String idNumber;

}
