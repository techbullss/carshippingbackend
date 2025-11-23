package io.reflectoring.carshippingbackend.controllers;
import io.reflectoring.carshippingbackend.DTO.UpdateRoleRequest;
import io.reflectoring.carshippingbackend.DTO.UpdateUserRequest;
import io.reflectoring.carshippingbackend.DTO.UserResponse;
import io.reflectoring.carshippingbackend.services.UserService;
import io.reflectoring.carshippingbackend.tables.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "https://f-carshipping.com")
@RequestMapping("/api/admin")
public class AdminController {
    @Autowired
    private final UserService userService;

    /**
     * ============================
     * 🔹 GET ALL USERS (Admin Only)
     * ============================
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {

        try {
            if (search != null && !search.trim().isEmpty()) {
                Page<User> userPage = userService.searchUsers(search, PageRequest.of(page, size));
                return ResponseEntity.ok(Map.of(
                        "content", userPage.getContent().stream()
                                .map(this::convertToUserResponse)
                                .collect(Collectors.toList()),
                        "totalElements", userPage.getTotalElements(),
                        "totalPages", userPage.getTotalPages(),
                        "currentPage", page
                ));
            } else {
                Page<User> userPage = (Page<User>) userService.findAllUsers(PageRequest.of(page, size));
                return ResponseEntity.ok(Map.of(
                        "content", userPage.getContent().stream()
                                .map(this::convertToUserResponse)
                                .collect(Collectors.toList()),
                        "totalElements", userPage.getTotalElements(),
                        "totalPages", userPage.getTotalPages(),
                        "currentPage", page
                ));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch users", e);
        }
    }

    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("Test endpoint works!");
    }
    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        User user = userService.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return ResponseEntity.ok(convertToUserResponse(user));
    }

    /**
     * ============================
     *  UPDATE USER
     * ============================
     */
    @PutMapping("/users/{id}")

    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @RequestBody UpdateUserRequest request) {

        User updatedUser = userService.updateUser(id, request);
        return ResponseEntity.ok(convertToUserResponse(updatedUser));
    }

    /**
     * ============================
     *  DELETE USER
     * ============================
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().body("User deleted successfully");
    }

    /**
     * ============================
     * 🔹 UPDATE USER ROLES
     * ============================
     */
    @PutMapping("/users/roles/{id}")
    public ResponseEntity<UserResponse> updateUserRoles(
            @PathVariable Long id,
            @RequestBody UpdateRoleRequest request) {

        User updatedUser = userService.updateUserRoles(id, request.getRole(), request.getAction());
        return ResponseEntity.ok(convertToUserResponse(updatedUser));
    }
    @PutMapping("/users/approve/{userId}")
    public ResponseEntity<UserResponse> approveUser(@PathVariable Long userId) {
        try {
            User approvedUser = userService.approveUser(userId);
            return ResponseEntity.ok(convertToUserResponse(approvedUser));
        } catch (Exception e) {
            throw new RuntimeException("Failed to approve user: " + e.getMessage(), e);
        }
    }

    private UserResponse convertToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender())
                .streetAddress(user.getStreetAddress())
                .city(user.getCity())
                .state(user.getState())
                .postalCode(user.getPostalCode())
                .country(user.getCountry())
                .preferredCommunication(user.getPreferredCommunication())
                .newsletter(user.isNewsletter())
                .verificationCode(user.getVerificationCode()) // Missing
                .emailVerified(user.isEmailVerified()) // Missing
                .shippingFrequency(user.getShippingFrequency())
                .vehicleType(user.getVehicleType())
                .estimatedShippingDate(user.getEstimatedShippingDate())
                .sourceCountry(user.getSourceCountry())
                .destinationCountry(user.getDestinationCountry())
                .roles(user.getRoles().stream()
                        .map(Enum::name)
                        .collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .status(user.getStatus()) // Missing
                .passportPhoto(user.getPassportPhoto()) // Missing
                .govtId(user.getGovtId()) // Missing
                .idNumber(user.getIdNumber()) // Missing
                .build();
    }
}