package io.reflectoring.carshippingbackend.DTO;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.Set;

@Data
public class SignupRequest {
    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name cannot exceed 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name cannot exceed 50 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Size(max = 15, message = "Phone number cannot exceed 15 characters")
    private String phone;

    private String dateOfBirth;
    private String gender;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;

    // Address fields
    private String streetAddress;
    private String city;
    private String state;
    private String postalCode;
    private String country;

    // Preferences
    private Set<String> preferredCommunication;
    private boolean newsletter;

    // Vehicle shipping preferences
    private String shippingFrequency;
    private String vehicleType;
    private String estimatedShippingDate;
    private String sourceCountry = "UK";
    private String destinationCountry = "Kenya";
    private String role = "SELLER";
}
