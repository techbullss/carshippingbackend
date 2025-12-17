package io.reflectoring.carshippingbackend.controllers;

import io.reflectoring.carshippingbackend.DTO.MotorcycleResponseDTO;
import io.reflectoring.carshippingbackend.services.MotorcycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "https://f-carshipping.com")
@RequestMapping("/api/motorcycles")
@RequiredArgsConstructor
public class MotorcycleController {

    private final MotorcycleService service;

    // ============= SEARCH / LIST (Public - APPROVED only) =============
    @GetMapping
    public ResponseEntity<?> search(
            @RequestParam Map<String, String> allParams,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        try {
            String[] sortParts = sort.split(",");
            org.springframework.data.domain.Sort s = org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Direction.fromString(
                            sortParts.length > 1 ? sortParts[1] : "desc"),
                    sortParts[0]);

            // Only return APPROVED motorcycles for public
            var result = service.searchApproved(allParams, page, size, s);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============= DASHBOARD (Role-based access) =============
    @PostMapping("/dashboard")
    public ResponseEntity<?> dashboard(
            @RequestParam Map<String, String> allParams,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestBody Map<String, String> userPayload
    ) {
        try {
            String email = userPayload.get("email");
            String role = userPayload.get("role");

            if (email == null || role == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Missing email or role");
            }

            String[] sortParts = sort.split(",");
            org.springframework.data.domain.Sort s = org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Direction.fromString(
                            sortParts.length > 1 ? sortParts[1] : "desc"),
                    sortParts[0]);

            Page<MotorcycleResponseDTO> motorcycles = service.searchByUserRole(
                    allParams, page, size, s, email, role);

            return ResponseEntity.ok(motorcycles);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch motorcycles: " + e.getMessage());
        }
    }

    // ============= CREATE =============
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
            @RequestPart("motorcycle") Map<String, String> motorcycleData,
            @RequestPart(value = "images", required = false) MultipartFile[] images,
            Authentication authentication
    ) throws IOException {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body("Authentication required");
            }

            String userEmail = authentication.getName();
            String userRole = authentication.getAuthorities().iterator().next().getAuthority();

            MotorcycleResponseDTO created = service.createMotorcycle(motorcycleData, images, userEmail, userRole);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============= GET BY ID =============
    @GetMapping("/{id}")
    public ResponseEntity<?> getMotorcycle(@PathVariable Long id) {
        try {
            MotorcycleResponseDTO motorcycle = service.getMotorcycle(id);
            return ResponseEntity.ok(motorcycle);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============= UPDATE =============
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateMotorcycle(
            @PathVariable Long id,
            @RequestPart("motorcycle") Map<String, String> motorcycleData,
            @RequestPart(value = "images", required = false) MultipartFile[] images,
            Authentication authentication
    ) throws IOException {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body("Authentication required");
            }

            MotorcycleResponseDTO updated = service.updateMotorcycle(id, motorcycleData, images);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============= DELETE =============
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMotorcycle(@PathVariable Long id, Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body("Authentication required");
            }

            service.deleteMotorcycle(id);
            return ResponseEntity.ok(Map.of("message", "Motorcycle deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============= APPROVE (Admin only) =============
    @PutMapping("/approve/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approveMotorcycle(@PathVariable Long id) {
        try {
            MotorcycleResponseDTO approved = service.approveMotorcycle(id);
            return ResponseEntity.ok(approved);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============= REJECT (Admin only) =============
    @PutMapping("/reject/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> rejectMotorcycle(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String reason = body.getOrDefault("reason", "No reason provided");
            MotorcycleResponseDTO rejected = service.rejectMotorcycle(id, reason);
            return ResponseEntity.ok(rejected);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============= SIMILAR MOTORCYCLES =============
    @GetMapping("/similar")
    public ResponseEntity<?> getSimilarMotorcycles(
            @RequestParam String brand,
            @RequestParam String model,
            @RequestParam(required = false) Long exclude
    ) {
        try {
            List<MotorcycleResponseDTO> similar = service.getSimilarMotorcycles(brand, model, exclude);
            return ResponseEntity.ok(similar);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============= LATEST ARRIVALS =============
    @GetMapping("/latest")
    public ResponseEntity<?> getLatestArrivals() {
        try {
            List<MotorcycleResponseDTO> latest = service.getLatestArrivals();
            return ResponseEntity.ok(latest);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============= GET ALL BRANDS =============
    @GetMapping("/brands")
    public ResponseEntity<?> getAllBrands() {
        try {
            List<Map<String, Object>> brands = service.getDistinctBrandsWithCount();
            return ResponseEntity.ok(brands);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============= GET MODELS BY BRAND =============
    @GetMapping("/models")
    public ResponseEntity<?> getModelsByBrand(@RequestParam String brand) {
        try {
            List<Map<String, Object>> models = service.getDistinctModelsByBrand(brand);
            return ResponseEntity.ok(models);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ============= LEGACY FILTER (Optional - keep if needed) =============
    @GetMapping("/filter")
    public ResponseEntity<?> filterMotorcycles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(required = false) String make,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String priceRange,
            @RequestParam(required = false) String year
    ) {
        try {
            Page<MotorcycleResponseDTO> result = service.filterMotorcycles(page, size, make, type, priceRange, year);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}