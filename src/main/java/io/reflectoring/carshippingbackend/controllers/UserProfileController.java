package io.reflectoring.carshippingbackend.controllers;

import io.reflectoring.carshippingbackend.DTO.ChangePasswordRequest;
import io.reflectoring.carshippingbackend.services.UserService;
import io.reflectoring.carshippingbackend.tables.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "https://f-carshipping.com/", allowCredentials = "true")
public class UserProfileController {

    private final UserService userService;

    /**
     * ============================
     * 🔹 GET CURRENT USER PROFILE
     * ============================
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        try {
            User currentUser = userService.getCurrentUser()
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("id", currentUser.getId());
            response.put("firstName", currentUser.getFirstName());
            response.put("lastName", currentUser.getLastName());
            response.put("email", currentUser.getEmail());
            response.put("phone", currentUser.getPhone());
            response.put("profilePicture", currentUser.getProfilePicture());
            response.put("roles", currentUser.getRoles().stream()
                    .map(Enum::name)
                    .collect(Collectors.toSet()));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to fetch user profile: " + e.getMessage());
        }
    }

    /**
     * ============================
     *  UPLOAD PROFILE PICTURE
     * ============================
     */
    @PostMapping("/{userId}/profile-picture")
    public ResponseEntity<?> uploadProfilePicture(
            @PathVariable Long userId,
            @RequestParam("profilePicture") MultipartFile file) {

        try {
            String profilePictureUrl = userService.updateProfilePicture(userId, file);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Profile picture uploaded successfully");
            response.put("profilePictureUrl", profilePictureUrl);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to upload profile picture: " + e.getMessage());
        }
    }

    /**
     * ============================
     *  DELETE PROFILE PICTURE
     * ============================
     */
    @DeleteMapping("/{userId}/profile-picture")
    public ResponseEntity<?> deleteProfilePicture(@PathVariable Long userId) {
        try {
            User user = userService.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (user.getProfilePicture() != null) {
                userService.deleteOldProfilePicture(user.getProfilePicture());
                user.setProfilePicture(null);
                userService.save(user); // You'll need to add a save method in UserService
            }

            Map<String, String> response = new HashMap<>();
            response.put("message", "Profile picture deleted successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to delete profile picture: " + e.getMessage());
        }
    }

    /**
     * ============================
     *  CHANGE PASSWORD
     * ============================
     */
    @PutMapping("/{userId}/password")
    public ResponseEntity<?> changePassword(
            @PathVariable Long userId,
            @RequestBody ChangePasswordRequest request) {

        try {
            userService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());

            Map<String, String> response = new HashMap<>();
            response.put("message", "Password changed successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to change password: " + e.getMessage());
        }
    }
}