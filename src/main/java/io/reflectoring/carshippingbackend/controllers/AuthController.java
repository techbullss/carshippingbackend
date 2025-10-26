package io.reflectoring.carshippingbackend.controllers;

import io.reflectoring.carshippingbackend.DTO.AuthResponse;
import io.reflectoring.carshippingbackend.DTO.LoginRequest;
import io.reflectoring.carshippingbackend.DTO.SignupRequest;
import io.reflectoring.carshippingbackend.Enum.Role;
import io.reflectoring.carshippingbackend.Util.JwtUtil;

import io.reflectoring.carshippingbackend.services.AuthService;
import io.reflectoring.carshippingbackend.services.CloudStorageService;
import io.reflectoring.carshippingbackend.services.EmailService;
import io.reflectoring.carshippingbackend.services.UserService;
import io.reflectoring.carshippingbackend.tables.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "https://f-carshipping.com/", allowCredentials = "true")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final CloudStorageService cloudStorageService;
    private final EmailService emailService;

    @Value("${app.jwt.cookie-name}")
    private String cookieName;

    @Value("${app.jwt.expiration}")
    private Long jwtExpiration;

    /**
     * ============================
     * 🔹 USER SIGNUP
     * ============================
     */
    @PostMapping(
            value = "/signup",
            consumes = { MediaType.MULTIPART_FORM_DATA_VALUE }
    )
    public ResponseEntity<AuthResponse> signup(
            @RequestPart("data") @Valid SignupRequest request,
            @RequestPart(value = "govtId", required = false) MultipartFile govtId,
            @RequestPart(value = "passportPhoto", required = false) MultipartFile passportPhoto,
            HttpServletResponse response) {

        System.out.println("=== SIGNUP REQUEST RECEIVED ===");
        System.out.println("Email: " + request.getEmail());

        try {
            Set<Role> roles = new HashSet<>();
            roles.add(Role.SELLER);
            request.setStatus("pending");
            String verificationCode = String.valueOf(new Random().nextInt(900000) + 100000);
            request.setVerificationCode(verificationCode);
            request.setEmailVerified(false);

            if (request.getRole() != null) {
                try {
                    Role givenRole = Role.valueOf(request.getRole().toUpperCase());
                    roles.clear();
                    roles.add(givenRole);
                } catch (IllegalArgumentException e) {
                    System.out.println("⚠️ Invalid role provided: " + request.getRole() + ". Defaulting to USER.");
                }
            }

            // 🔹 Upload files (if present)
            String govtIdUrl = null;
            String passportPhotoUrl = null;

            if (govtId != null && !govtId.isEmpty()) {
                govtIdUrl = cloudStorageService.uploadFile(govtId, "user-ids");
            }
            if (passportPhoto != null && !passportPhoto.isEmpty()) {
                passportPhotoUrl = cloudStorageService.uploadFile(passportPhoto, "passport-photos");
            }

            // 🔹 Set file URLs in request before registering
            request.setGovtId(govtIdUrl);
            request.setPassportPhoto(passportPhotoUrl);

            // 🔹 Register user
            AuthResponse authResponse = authService.registerUser(request, roles);
            emailService.sendVerificationEmail(request.getEmail(), verificationCode);

            // 🔹 Generate JWT & cookie
            String token = jwtUtil.generateToken(request.getEmail(), roles);
            setAuthCookie(response, token);

            System.out.println("✅ USER REGISTERED SUCCESSFULLY: " + request.getEmail());
            return ResponseEntity.ok(authResponse);

        } catch (Exception e) {
            System.err.println("❌ SIGNUP ERROR: " + e.getMessage());
            return ResponseEntity.badRequest().body(new AuthResponse("Registration failed: " + e.getMessage()));
        }
    }
    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");

        try {
            authService.verifyEmail(email, code);
            return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/resend-code")
    public ResponseEntity<?> resendVerificationCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        try {
            authService.resendVerificationCode(email);
            return ResponseEntity.ok(Map.of("message", "Verification code resent successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * ============================
     * 🔹 USER LOGIN
     * ============================
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletResponse response) {

        String token = authService.authenticateUser(request);

        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Set<String> roleNames = user.getRoles()
                .stream()
                .map(Enum::name)
                .collect(Collectors.toSet());
        setAuthCookie(response, token);

        AuthResponse authResponse = new AuthResponse(
                "Login successful",
                user.getId().toString(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                roleNames,
                user.getProfilePicture()
        );

        return ResponseEntity.ok(authResponse);
    }

    /**
     * ============================
     * 🔹 LOGOUT
     * ============================
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        clearAuthCookie(response);
        return ResponseEntity.ok().body("Logged out successfully");
    }

    /**
     * ============================
     * 🔹 TOKEN VALIDATION
     * ============================
     */
    @GetMapping("/validate")
    public ResponseEntity<AuthResponse> validateToken(HttpServletRequest request) {
        String token = getTokenFromRequest(request);

        if (token == null) {
            throw new RuntimeException("No authentication token found");
        }

        // Validate and get user
        User user = authService.validateToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        // Extract roles from JWT
        Set<String> roles = jwtUtil.extractRoles(token);

        AuthResponse authResponse = new AuthResponse(
                "Token is valid",
                user.getId().toString(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                roles,
                user.getProfilePicture()
        );

        return ResponseEntity.ok(authResponse);
    }


    /**
     * ============================
     * 🔹 HELPER METHODS
     * ============================
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private void setAuthCookie(HttpServletResponse response, String token) {
        boolean isLocalhost = false;

        String backendDomain = "api.f-carshipping.com";
        String cookieDomain = isLocalhost ? "localhost" : backendDomain;

        ResponseCookie cookie = ResponseCookie.from(cookieName, token)
                .httpOnly(true)
                .secure(!isLocalhost) // true in production, false locally
                .sameSite(isLocalhost ? "Lax" : "None") // must be None for cross-site (Vercel → DuckDNS)
                .domain(cookieDomain)
                .path("/")
                .maxAge(Duration.ofMillis(jwtExpiration))
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }


    private void clearAuthCookie(HttpServletResponse response) {
        boolean isLocalhost = false;
        String backendDomain = "api.f-carshipping.com";
        String cookieDomain = isLocalhost ? "localhost" : backendDomain;

        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(!isLocalhost) // Must match login cookie settings
                .sameSite(isLocalhost ? "Lax" : "None") // Must match login cookie settings
                .domain(cookieDomain) // Must match login cookie settings
                .path("/")
                .maxAge(0) // Set to 0 to expire immediately
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }
}
