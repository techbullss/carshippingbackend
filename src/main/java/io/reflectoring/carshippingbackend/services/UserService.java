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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Cloudinary cloudinary;
    private final EmailService emailService;

    public User createUser(SignupRequest signupRequest, Set<Role> roles) {
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            throw new RuntimeException("Email is already in use!");
        }

        User user = new User();

        // Basic info
        user.setFirstName(signupRequest.getFirstName());
        user.setLastName(signupRequest.getLastName());
        user.setEmail(signupRequest.getEmail());
        user.setPhone(signupRequest.getPhone());
        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));

        // Address
        user.setStreetAddress(signupRequest.getStreetAddress());
        user.setCity(signupRequest.getCity());
        user.setState(signupRequest.getState());
        user.setPostalCode(signupRequest.getPostalCode());
        user.setCountry(signupRequest.getCountry());

        // Documents
        user.setIdNumber(signupRequest.getIdNumber());
        user.setGovtId(signupRequest.getGovtId());
        user.setPassportPhoto(signupRequest.getPassportPhoto());

        // Preferences
        user.setPreferredCommunication(signupRequest.getPreferredCommunication());
        user.setNewsletter(signupRequest.isNewsletter());

        // Vehicle shipping
        user.setShippingFrequency(signupRequest.getShippingFrequency());
        user.setVehicleType(signupRequest.getVehicleType());
        user.setEstimatedShippingDate(signupRequest.getEstimatedShippingDate());
        user.setSourceCountry(signupRequest.getSourceCountry());
        user.setDestinationCountry(signupRequest.getDestinationCountry());

        // Account status
        user.setStatus(signupRequest.getStatus());
        user.setRoles(roles);
        user.setVerificationCode(signupRequest.getVerificationCode());
        user.setEmailVerified(signupRequest.getEmailVerified());

        // NEW: Seller type
        user.setSellerType(signupRequest.getSellerType());

        // NEW: Company fields
        user.setCompanyName(signupRequest.getCompanyName());
        user.setCompanyRegistrationNumber(signupRequest.getCompanyRegistrationNumber());
        user.setKraPin(signupRequest.getKraPin());
        user.setBusinessPermitNumber(signupRequest.getBusinessPermitNumber());
        user.setCompanyAddress(signupRequest.getCompanyAddress());

        // NEW: Company document URLs
        user.setCertificateOfIncorporation(signupRequest.getCertificateOfIncorporation());
        user.setKraPinCertificate(signupRequest.getKraPinCertificate());
        user.setBusinessPermit(signupRequest.getBusinessPermit());
        user.setTrademarkImage(signupRequest.getTrademarkImage());

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

        return new CustomUserDetails(user);
    }

    public User updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update basic fields if provided
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getPhone() != null) user.setPhone(request.getPhone());

        // Update address fields
        if (request.getStreetAddress() != null) user.setStreetAddress(request.getStreetAddress());
        if (request.getCity() != null) user.setCity(request.getCity());
        if (request.getState() != null) user.setState(request.getState());
        if (request.getPostalCode() != null) user.setPostalCode(request.getPostalCode());
        if (request.getCountry() != null) user.setCountry(request.getCountry());

        // Update preferences
        if (request.getNewsletter() != null) user.setNewsletter(request.getNewsletter());
        if (request.getShippingFrequency() != null) user.setShippingFrequency(request.getShippingFrequency());
        if (request.getVehicleType() != null) user.setVehicleType(request.getVehicleType());
        if (request.getEstimatedShippingDate() != null) user.setEstimatedShippingDate(request.getEstimatedShippingDate());

        // Update document info

        // Update company fields if provided
        if (request.getSellerType() != null) user.setSellerType(request.getSellerType());
        if (request.getCompanyName() != null) user.setCompanyName(request.getCompanyName());
        if (request.getCompanyRegistrationNumber() != null) user.setCompanyRegistrationNumber(request.getCompanyRegistrationNumber());
        if (request.getKraPin() != null) user.setKraPin(request.getKraPin());
        if (request.getBusinessPermitNumber() != null) user.setBusinessPermitNumber(request.getBusinessPermitNumber());
        if (request.getCompanyAddress() != null) user.setCompanyAddress(request.getCompanyAddress());

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
                            "folder", "user-profiles"
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
                String publicId = extractPublicIdFromUrl(oldImageUrl);
                if (publicId != null) {
                    cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                }
            } catch (Exception e) {
                System.err.println("Failed to delete old profile picture: " + e.getMessage());
            }
        }
    }

    private String extractPublicIdFromUrl(String imageUrl) {
        try {
            String[] parts = imageUrl.split("/upload/");
            if (parts.length > 1) {
                String withVersion = parts[1];
                String withoutVersion = withVersion.replaceFirst("v\\d+/", "");
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

        if (user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()) {
            deleteOldProfilePicture(user.getProfilePicture());
        }

        return saveProfilePicture(userId, file);
    }

    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        if (newPassword.length() < 8) {
            throw new RuntimeException("New password must be at least 8 characters long");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    /**
     * ============================
     *  GET CURRENT USER FROM JWT TOKEN
     * ============================
     */
    public Optional<User> getCurrentUserFromToken() {
        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.empty();
            }

            String username = authentication.getName();

            if ("anonymousUser".equals(username)) {
                return Optional.empty();
            }

            return findByEmail(username);

        } catch (Exception e) {
            System.err.println("Error getting current user from token: " + e.getMessage());
            return Optional.empty();
        }
    }

    public User approveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        user.setStatus("approved");
        user.setUpdatedAt(LocalDateTime.now());
        emailService.sendApprovalEmail(
                user.getEmail(),
                user.getFirstName()
        );

        return userRepository.save(user);
    }

    public Page<User> findAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public Page<User> searchUsers(String searchTerm, Pageable pageable) {
        return userRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                searchTerm, searchTerm, searchTerm, pageable);
    }
}