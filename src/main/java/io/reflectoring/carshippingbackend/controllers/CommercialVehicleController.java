package io.reflectoring.carshippingbackend.controllers;

import io.reflectoring.carshippingbackend.Enum.Role;
import io.reflectoring.carshippingbackend.configaration.CustomUserDetails;
import io.reflectoring.carshippingbackend.repository.CommercialVehicleRepository;
import io.reflectoring.carshippingbackend.services.CommercialVehicleService;
import io.reflectoring.carshippingbackend.services.CommercialVehicleSpecification;
import io.reflectoring.carshippingbackend.tables.Car;
import io.reflectoring.carshippingbackend.tables.CommercialVehicle;
import io.reflectoring.carshippingbackend.DTO.CommercialVehicleDTO;
import io.reflectoring.carshippingbackend.DTO.CommercialVehicleResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vehicles")
@CrossOrigin(origins = "https://f-carshipping.com")
@RequiredArgsConstructor
public class CommercialVehicleController {

    private final CommercialVehicleService service;
    private final CommercialVehicleRepository repo;

    // ------------------- Search / List -------------------
    // ------------------- SIMPLER VERSION -------------------
    @GetMapping
    public ResponseEntity<Page<CommercialVehicleResponseDTO>> searchVehicles(
            @RequestParam Map<String, String> allParams,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "priceKes,desc") String sort) {

        String[] sortParts = sort.split(",");
        Sort s = Sort.by(Sort.Direction.fromString(sortParts.length > 1 ? sortParts[1] : "desc"), sortParts[0]);

        // Filter out pagination/sorting parameters
        Map<String, String> filters = allParams.entrySet().stream()
                .filter(entry -> !entry.getKey().equals("page") &&
                        !entry.getKey().equals("size") &&
                        !entry.getKey().equals("sort"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // Add APPROVED status for public access
        if (!filters.containsKey("status")) {
            filters.put("status", "APPROVED");
        }

        Pageable pageable = PageRequest.of(page, size, s);
        Specification<CommercialVehicle> spec = CommercialVehicleSpecification.byFilters(filters);
        Page<CommercialVehicle> results = repo.findAll(spec, pageable);

        return ResponseEntity.ok(results.map(service::toDto));
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
        dto.setStatus("PENDING");
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
    @PostMapping("/dashboard")
    public ResponseEntity<?> dashboard(
            @RequestBody Map<String, Object> payload
    ) {
        try {
            String email = (String) payload.get("email");
            String role = (String) payload.get("role");

            int page = (int) payload.getOrDefault("page", 0);
            int size = (int) payload.getOrDefault("size", 12);
            String search = (String) payload.getOrDefault("search", "");
            String type = (String) payload.getOrDefault("type", "");

            Sort sortObj = Sort.by(Sort.Order.desc("priceKes"));

            Page<CommercialVehicle> cars =
                    service.searchByUserRole(
                            Map.of("search", search, "type", type),
                            page, size, sortObj, email, role
                    );

            return ResponseEntity.ok(cars);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch cars: " + e.getMessage());
        }
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
