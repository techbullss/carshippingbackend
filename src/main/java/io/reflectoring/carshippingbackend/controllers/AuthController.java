package io.reflectoring.carshippingbackend.controllers;

import io.reflectoring.carshippingbackend.DTO.AuthResponse;
import io.reflectoring.carshippingbackend.DTO.LoginRequest;
import io.reflectoring.carshippingbackend.DTO.SignupRequest;
import io.reflectoring.carshippingbackend.Enum.Role;
import io.reflectoring.carshippingbackend.Util.JwtUtil;

import io.reflectoring.carshippingbackend.services.AuthService;
import io.reflectoring.carshippingbackend.services.UserService;
import io.reflectoring.carshippingbackend.tables.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashSet;
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


    @Value("${app.jwt.cookie-name}")
    private String cookieName;

    @Value("${app.jwt.expiration}")
    private Long jwtExpiration;

    /**
     * ============================
     * 🔹 USER SIGNUP
     * ============================
     */
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(
            @RequestBody @Valid SignupRequest request,
            HttpServletResponse response) {

        System.out.println("=== SIGNUP REQUEST RECEIVED ===");
        System.out.println("Email: " + request.getEmail());

        try {
            //  Automatically assign default USER role
            Set<Role> roles = new HashSet<>();
            roles.add(Role.SELLER);

            // Optional: If you want to allow specifying role from frontend for testing/admin setup
            if (request.getRole() != null) {
                try {
                    Role givenRole = Role.valueOf(request.getRole().toUpperCase());
                    roles.clear();
                    roles.add(givenRole);
                } catch (IllegalArgumentException e) {
                    System.out.println("⚠️ Invalid role provided: " + request.getRole() + ". Defaulting to USER.");
                }
            }

            // Register user
            AuthResponse authResponse = authService.registerUser(request, roles);

            // Generate JWT token
            String token = jwtUtil.generateToken(request.getEmail(),roles);
            setAuthCookie(response, token);

            System.out.println("✅ USER REGISTERED SUCCESSFULLY: " + request.getEmail());
            return ResponseEntity.ok(authResponse);

        } catch (Exception e) {
            System.err.println("❌ SIGNUP ERROR: " + e.getMessage());
            throw new RuntimeException("Registration failed: " + e.getMessage());
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
                roleNames
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
                roles
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
