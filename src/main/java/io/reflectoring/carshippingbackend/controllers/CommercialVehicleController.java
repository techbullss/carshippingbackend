package io.reflectoring.carshippingbackend.controllers;

import io.reflectoring.carshippingbackend.services.CommercialVehicleService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vehicles")
@CrossOrigin(origins = "https://f-carshipping.com")
@RequiredArgsConstructor
public class CommercialVehicleController {

    private final CommercialVehicleService service;

    // ------------------- Search / List -------------------
    @GetMapping
    public ResponseEntity<Page<CommercialVehicleResponseDTO>> search(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "priceKes,desc") String sort,
            @RequestParam Map<String, String> allParams  // Capture ALL query parameters
    ) {
        String[] sortParts = sort.split(",");
        Sort s = Sort.by(Sort.Direction.fromString(sortParts.length > 1 ? sortParts[1] : "desc"), sortParts[0]);

        // Filter out pagination and sorting parameters from filters
        Map<String, String> filters = allParams.entrySet().stream()
                .filter(entry -> !isPaginationOrSortParam(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // Add default status filter for public search
        if (!filters.containsKey("status")) {
            filters.put("status", "APPROVED");
        }

        Page<CommercialVehicleResponseDTO> results = service.searchVehicles(page, size, filters, s);
        return ResponseEntity.ok(results);
    }

    private boolean isPaginationOrSortParam(String key) {
        return key.equals("page") || key.equals("size") || key.equals("sort");
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

    // ------------------- Dashboard -------------------
    @PostMapping("/dashboard")
    public ResponseEntity<?> dashboard(
            @RequestBody Map<String, Object> payload,
            Authentication authentication
    ) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body("Authentication required");
            }

            String email = authentication.getName();
            String role = authentication.getAuthorities().iterator().next().getAuthority();

            int page = (int) payload.getOrDefault("page", 0);
            int size = (int) payload.getOrDefault("size", 12);

            // Extract filters from payload
            @SuppressWarnings("unchecked")
            Map<String, String> filters = (Map<String, String>) payload.getOrDefault("filters", new HashMap<>());

            // For sellers, add their email filter
            if (role.equals("ROLE_SELLER")) {
                filters.put("seller", email);
            }

            Sort sortObj = Sort.by(Sort.Order.desc("priceKes"));

            Page<CommercialVehicleResponseDTO> vehicles =
                    service.searchByUserRole(
                            filters, page, size, sortObj, email, role
                    ).map(service::toDto);

            return ResponseEntity.ok(vehicles);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch vehicles: " + e.getMessage());
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

    // ------------------- Get Filter Options -------------------
    @GetMapping("/filters/options")
    public ResponseEntity<Map<String, List<String>>> getFilterOptions() {
        return ResponseEntity.ok(service.getFilterOptions());
    }
}