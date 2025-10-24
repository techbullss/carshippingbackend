package io.reflectoring.carshippingbackend.services;

import io.reflectoring.carshippingbackend.DTO.AuthResponse;
import io.reflectoring.carshippingbackend.DTO.LoginRequest;
import io.reflectoring.carshippingbackend.DTO.SignupRequest;
import io.reflectoring.carshippingbackend.Enum.Role;
import io.reflectoring.carshippingbackend.Util.JwtUtil;
import io.reflectoring.carshippingbackend.tables.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse registerUser(SignupRequest signupRequest, Set<Role> roles) {
        User user = userService.createUser(signupRequest,roles);
        Set<String> roleNames = user.getRoles()
                .stream()
                .map(Enum::name)
                .collect(Collectors.toSet());
        return new AuthResponse(
                "User registered successfully",
                user.getId().toString(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                roleNames,
                user.getProfilePicture()
        );
    }

    public String authenticateUser(LoginRequest loginRequest) {
        Optional<User> userOptional = userService.findByEmail(loginRequest.getEmail());

        if (userOptional.isEmpty()) {
            throw new RuntimeException("User not found with email: " + loginRequest.getEmail());
        }

        User user = userOptional.get();

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        // ✅ Directly get roles (already Set<Role>)
        Set<Role> roles = user.getRoles();

        // ✅ Generate JWT including roles
        return jwtUtil.generateToken(user.getEmail(), roles);
    }


    public Optional<User> validateToken(String token) {
        try {
            if (!jwtUtil.validateToken(token)) {
                return Optional.empty();
            }
            String email = jwtUtil.extractEmail(token);
            return userService.findByEmail(email);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}