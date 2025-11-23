package io.reflectoring.carshippingbackend.controllers;

import io.reflectoring.carshippingbackend.Enum.Role;
import io.reflectoring.carshippingbackend.configaration.CustomUserDetails;
import io.reflectoring.carshippingbackend.services.CommercialVehicleService;
import io.reflectoring.carshippingbackend.tables.CommercialVehicle;
import io.reflectoring.carshippingbackend.DTO.CommercialVehicleDTO;
import io.reflectoring.carshippingbackend.DTO.CommercialVehicleResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
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
import java.util.Optional;

@RestController
@RequestMapping("/api/vehicles")
@CrossOrigin(origins = "https://f-carshipping.com/")
@RequiredArgsConstructor
public class CommercialVehicleController {

    private final CommercialVehicleService service;

    // ------------------- Search / List -------------------
    @GetMapping
    public ResponseEntity<Page<CommercialVehicleResponseDTO>> search(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "priceKes,desc") String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type
    ) {
        String[] sortParts = sort.split(",");
        Sort s = Sort.by(Sort.Direction.fromString(sortParts.length > 1 ? sortParts[1] : "desc"), sortParts[0]);
        return ResponseEntity.ok(service.searchVehicles(page, size, search, type, s));
    }

    // ------------------- Create -------------------
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
            @RequestPart("vehicle") CommercialVehicleDTO dto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            Authentication authentication
    ) throws IOException {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Authentication required");
        }
        String userEmail = authentication.getName();
        String userRole = authentication.getAuthorities().iterator().next().getAuthority();
        dto.setImages(images);
        CommercialVehicleResponseDTO created = service.createVehicle(dto, userEmail, userRole);
        return ResponseEntity.ok(created);
    }

    // ------------------- Get by ID -------------------
    @GetMapping("/{id}")
    public ResponseEntity<?> getVehicle(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.getVehicle(id));
        } catch (Exception e) {
            return ResponseEntity.status(404).body("Vehicle not found with id " + id);
        }
    }

    // ------------------- Update -------------------
    @PutMapping(value="/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestPart("vehicle") CommercialVehicleDTO dto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            Authentication authentication
    ) throws IOException {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Authentication required");
        }
        dto.setImages(images);
        return ResponseEntity.ok(service.updateVehicle(id, dto));
    }

    // ------------------- Approve -------------------
    @PutMapping("/approve/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approve(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.approveVehicle(id));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ------------------- Reject -------------------
    @PutMapping("/reject/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "No reason provided");
        try {
            return ResponseEntity.ok(service.rejectVehicle(id, reason));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ------------------- Delete -------------------
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Authentication required");
        }
        service.deleteVehicle(id);
        return ResponseEntity.ok(Map.of("message", "Vehicle deleted successfully"));
    }

    // ------------------- Dashboard by User -------------------
    @PostMapping("/dashboard")
    public ResponseEntity<?> dashboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "priceKes,desc") String sort,
            @RequestBody Map<String, String> userPayload
    ) {
        try {
            String email = userPayload.get("email");
            String role = userPayload.get("role");
            if (email == null || role == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Missing email or role");
            }
            return ResponseEntity.ok(service.dashboardByUser(email, role, page, size));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ------------------- Latest Arrivals -------------------
    @GetMapping("/latest")
    public ResponseEntity<List<CommercialVehicleResponseDTO>> latest() {
        return ResponseEntity.ok(service.getLatestArrivals());
    }

    // ------------------- Similar Vehicles -------------------
    @GetMapping("/similar")
    public ResponseEntity<List<CommercialVehicleResponseDTO>> similar(
            @RequestParam String brand,
            @RequestParam String model,
            @RequestParam(required = false) Long exclude
    ) {
        return ResponseEntity.ok(service.getSimilarVehicles(brand, model, exclude));
    }
}
