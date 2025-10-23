package io.reflectoring.carshippingbackend.tables;

import io.reflectoring.carshippingbackend.Enum.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
    @SequenceGenerator(name = "user_seq", sequenceName = "user_seq", allocationSize = 1)
    private Long id;

    @NotBlank
    @Size(max = 50)
    @Column(name = "first_name")
    private String firstName;

    @NotBlank
    @Size(max = 50)
    @Column(name = "last_name")
    private String lastName;

    @NotBlank
    @Size(max = 100)
    @Email
    @Column(unique = true)
    private String email;

    @NotBlank
    @Size(max = 15)
    private String phone;

    @Column(name = "date_of_birth")
    private String dateOfBirth;

    private String gender;

    @NotBlank
    @Size(min = 8)
    private String password;

    // Address fields
    @Column(name = "street_address")
    private String streetAddress;

    private String city;
    private String state;

    @Column(name = "postal_code")
    private String postalCode;

    private String country;

    // Preferences
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "user_communication_preferences",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "communication_method", length = 50)
    private Set<String> preferredCommunication = new HashSet<>();

    @Column(name = "newsletter_subscribed")
    private boolean newsletter = false;

    // Vehicle shipping preferences
    @Column(name = "shipping_frequency")
    private String shippingFrequency;

    @Column(name = "vehicle_type")
    private String vehicleType;

    @Column(name = "estimated_shipping_date")
    private String estimatedShippingDate;

    @Column(name = "source_country")
    private String sourceCountry = "UK";

    @Column(name = "destination_country")
    private String destinationCountry = "Kenya";
    @Column(name = "profile_picture")
    private String profilePicture;

    // Roles (ADMIN, SELLER, ASSISTANT, USER)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20)
    private Set<Role> roles = new HashSet<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}