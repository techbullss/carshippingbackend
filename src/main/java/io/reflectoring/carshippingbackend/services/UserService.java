package io.reflectoring.carshippingbackend.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import io.reflectoring.carshippingbackend.DTO.SignupRequest;
import io.reflectoring.carshippingbackend.DTO.UpdateUserRequest;
import io.reflectoring.carshippingbackend.Enum.Role;
import io.reflectoring.carshippingbackend.configaration.CustomUserDetails;
import io.reflectoring.carshippingbackend.repository.UserRepository;
import io.reflectoring.carshippingbackend.tables.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Cloudinary cloudinary;

    public User createUser(SignupRequest signupRequest, Set<Role> roles) {
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            throw new RuntimeException("Email is already in use!");
        }

        User user = new User();
        user.setFirstName(signupRequest.getFirstName());
        user.setLastName(signupRequest.getLastName());
        user.setEmail(signupRequest.getEmail());
        user.setPhone(signupRequest.getPhone());
        user.setDateOfBirth(signupRequest.getDateOfBirth());
        user.setGender(signupRequest.getGender());
        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
        user.setStreetAddress(signupRequest.getStreetAddress());
        user.setCity(signupRequest.getCity());
        user.setState(signupRequest.getState());
        user.setPostalCode(signupRequest.getPostalCode());
        user.setCountry(signupRequest.getCountry());
        user.setPreferredCommunication(signupRequest.getPreferredCommunication());
        user.setNewsletter(signupRequest.isNewsletter());
        user.setShippingFrequency(signupRequest.getShippingFrequency());
        user.setVehicleType(signupRequest.getVehicleType());
        user.setEstimatedShippingDate(signupRequest.getEstimatedShippingDate());
        user.setSourceCountry(signupRequest.getSourceCountry());
        user.setDestinationCountry(signupRequest.getDestinationCountry());
        user.setRoles(roles);

        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }


    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return new CustomUserDetails(user); // wrap entity inside CustomUserDetails
    }
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }



    public User updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update fields if provided in request
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getDateOfBirth() != null) user.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getStreetAddress() != null) user.setStreetAddress(request.getStreetAddress());
        if (request.getCity() != null) user.setCity(request.getCity());
        if (request.getState() != null) user.setState(request.getState());
        if (request.getPostalCode() != null) user.setPostalCode(request.getPostalCode());
        if (request.getCountry() != null) user.setCountry(request.getCountry());
        if (request.getNewsletter() != null) user.setNewsletter(request.getNewsletter());
        if (request.getShippingFrequency() != null) user.setShippingFrequency(request.getShippingFrequency());
        if (request.getVehicleType() != null) user.setVehicleType(request.getVehicleType());
        if (request.getEstimatedShippingDate() != null) user.setEstimatedShippingDate(request.getEstimatedShippingDate());

        return userRepository.save(user);
    }

    public User updateUserRoles(Long id, String role, String action) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            Role roleEnum = Role.valueOf(role.toUpperCase());

            if ("ADD".equalsIgnoreCase(action)) {
                user.getRoles().add(roleEnum);
            } else if ("REMOVE".equalsIgnoreCase(action)) {
                user.getRoles().remove(roleEnum);
            } else {
                throw new RuntimeException("Invalid action. Use 'ADD' or 'REMOVE'");
            }

            return userRepository.save(user);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid role: " + role);
        }
    }

    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        userRepository.delete(user);
    }
    public String saveProfilePicture(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            // Validate file
            if (file.isEmpty()) {
                throw new RuntimeException("File is empty");
            }

            if (!file.getContentType().startsWith("image/")) {
                throw new RuntimeException("Only image files are allowed");
            }

            if (file.getSize() > 5 * 1024 * 1024) { // 5MB limit
                throw new RuntimeException("File size must be less than 5MB");
            }

            // Generate unique filename
            String uniqueFileName = UUID.randomUUID() + "-" +
                    Objects.requireNonNull(file.getOriginalFilename()).replaceAll("\\s+", "_");

            // Upload to Cloudinary
            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", "profile-pictures/" + uniqueFileName,
                            "resource_type", "auto",
                            "folder", "user-profiles" // Optional: organize in folder
                    )
            );

            String profilePictureUrl = (String) uploadResult.get("secure_url");

            // Update user profile picture
            user.setProfilePicture(profilePictureUrl);
            userRepository.save(user);

            return profilePictureUrl;

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload profile picture: " + e.getMessage());
        }
    }
    public void deleteOldProfilePicture(String oldImageUrl) {
        if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
            try {
                // Extract public_id from URL
                String publicId = extractPublicIdFromUrl(oldImageUrl);
                if (publicId != null) {
                    cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                }
            } catch (Exception e) {
                // Log the error but don't throw - we don't want to fail the update if delete fails
                System.err.println("Failed to delete old profile picture: " + e.getMessage());
            }
        }
    }
    private String extractPublicIdFromUrl(String imageUrl) {
        try {
            // Cloudinary URL pattern: https://res.cloudinary.com/cloudname/image/upload/v1234567/public_id.jpg
            String[] parts = imageUrl.split("/upload/");
            if (parts.length > 1) {
                String withVersion = parts[1];
                // Remove version part (v1234567/)
                String withoutVersion = withVersion.replaceFirst("v\\d+/", "");
                // Remove file extension
                return withoutVersion.substring(0, withoutVersion.lastIndexOf('.'));
            }
        } catch (Exception e) {
            System.err.println("Failed to extract public_id from URL: " + imageUrl);
        }
        return null;
    }
    public String updateProfilePicture(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Delete old profile picture if exists
        if (user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()) {
            deleteOldProfilePicture(user.getProfilePicture());
        }

        // Upload new profile picture
        return saveProfilePicture(userId, file);
    }
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        // Validate new password
        if (newPassword.length() < 8) {
            throw new RuntimeException("New password must be at least 8 characters long");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
    public User save(User user) {
        return userRepository.save(user);
    }
    /**
     * ============================
     * 🔹 GET CURRENT USER FROM JWT TOKEN
     * ============================
     */
    public Optional<User> getCurrentUserFromToken() {
        try {
            // Get authentication from security context
            var authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.empty();
            }

            String username = authentication.getName();

            // If username is "anonymousUser", return empty
            if ("anonymousUser".equals(username)) {
                return Optional.empty();
            }

            // Find user by email (username in JWT is typically the email)
            return findByEmail(username);

        } catch (Exception e) {
            System.err.println("Error getting current user from token: " + e.getMessage());
            return Optional.empty();
        }
    }
}
