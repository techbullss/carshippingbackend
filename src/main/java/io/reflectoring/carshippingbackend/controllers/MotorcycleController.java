package io.reflectoring.carshippingbackend.controllers;

import io.reflectoring.carshippingbackend.DTO.MotorcycleRequestDTO;
import io.reflectoring.carshippingbackend.DTO.MotorcycleResponseDTO;
import io.reflectoring.carshippingbackend.services.MotorcycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/motorcycles")
@CrossOrigin(origins = "https://f-carshipping.com")
@RequiredArgsConstructor
public class MotorcycleController {

    private final MotorcycleService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
            @RequestPart("motorcycle") MotorcycleRequestDTO dto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            Authentication authentication
    ) throws IOException {

        System.out.println("=== CREATE MOTORCYCLE WITH DTO ===");

        // Check authentication
        if (authentication != null && authentication.isAuthenticated()) {
            String userEmail = authentication.getName();
            dto.setOwner(userEmail);
            System.out.println("Setting owner to authenticated user: " + userEmail);
        }

        // Set images from request part
        dto.setImages(images);
        // Set initial status to PENDING
        dto.setStatus("PENDING");

        try {
            MotorcycleResponseDTO created = service.createMotorcycle(dto);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            System.err.println("Error creating motorcycle: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to create motorcycle",
                    "message", e.getMessage()
            ));
        }
    }

    // PUBLIC WEBSITE ENDPOINT - Only approved vehicles
    @GetMapping("/public")
    public ResponseEntity<?> listPublic(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String priceRange,
            @RequestParam(required = false) String year
    ) {
        Map<String, String> filters = new HashMap<>();
        if (search != null && !search.isBlank()) filters.put("search", search);
        if (type != null && !type.isBlank()) filters.put("type", type);
        if (brand != null && !brand.isBlank()) filters.put("brand", brand);
        if (priceRange != null && !priceRange.isBlank()) filters.put("priceRange", priceRange);
        if (year != null && !year.isBlank()) filters.put("year", year);

        // Force APPROVED status
        filters.put("status", "APPROVED");

        Page<MotorcycleResponseDTO> p = service.searchByFilters(filters, page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(p);
    }

    // GENERAL LISTING ENDPOINT - Defaults to approved
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status
    ) {
        String actualStatus = (status != null && !status.isBlank()) ? status : "APPROVED";
        Page<MotorcycleResponseDTO> p = service.search(page, size, search, type, actualStatus);
        return ResponseEntity.ok(p);
    }

    // DASHBOARD ENDPOINT - Role-based access (Admin/Seller)
    @GetMapping("/dashboard")
    public ResponseEntity<?> listDashboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String owner,
            Authentication authentication
    ) {
        String currentUserEmail = authentication.getName();
        String currentUserRole = authentication.getAuthorities().iterator().next().getAuthority();

        Map<String, String> filters = new HashMap<>();
        if (search != null && !search.isBlank()) filters.put("search", search);
        if (type != null && !type.isBlank()) filters.put("type", type);
        if (status != null && !status.isBlank()) filters.put("status", status);
        if (brand != null && !brand.isBlank()) filters.put("brand", brand);
        if (owner != null && !owner.isBlank()) filters.put("owner", owner);

        Page<MotorcycleResponseDTO> p = service.searchByUserRole(
                filters, page, size, Sort.by(Sort.Direction.DESC, "createdAt"),
                currentUserEmail, currentUserRole
        );

        return ResponseEntity.ok(p);
    }

    // LEGACY FILTER ENDPOINT - Only approved vehicles
    @GetMapping("/filter")
    public ResponseEntity<?> filter(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(required = false) String make,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String priceRange,
            @RequestParam(required = false) String year
    ) {
        Page<MotorcycleResponseDTO> p = service.filterMotorcycles(page, size, make, type, priceRange, year);
        return ResponseEntity.ok(p);
    }

    // GET SINGLE MOTORCYCLE
    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id) {
        try {
            MotorcycleResponseDTO motorcycle = service.getOne(id);
            return ResponseEntity.ok(motorcycle);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "error", "Motorcycle not found",
                            "message", e.getMessage()
                    ));
        }
    }

    // UPDATE MOTORCYCLE
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestPart("motorcycle") MotorcycleRequestDTO dto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            Authentication authentication
    ) throws IOException {
        try {
            // Set owner from authenticated user if available
            if (authentication != null && authentication.isAuthenticated()) {
                dto.setOwner(authentication.getName());
            }

            dto.setImages(images);

            // Convert DTO to Map
            Map<String, String> motorcycleData = new HashMap<>();
            motorcycleData.put("brand", dto.getBrand());
            motorcycleData.put("model", dto.getModel());
            motorcycleData.put("type", dto.getType());
            motorcycleData.put("status", dto.getStatus());
            motorcycleData.put("location", dto.getLocation());
            motorcycleData.put("description", dto.getDescription());
            motorcycleData.put("owner", dto.getOwner());

            if (dto.getEngineCapacity() != null) {
                motorcycleData.put("engineCapacity", dto.getEngineCapacity().toString());
            }
            if (dto.getPrice() != null) {
                motorcycleData.put("price", dto.getPrice().toString());
            }
            if (dto.getYear() != null) {
                motorcycleData.put("year", dto.getYear().toString());
            }
            if (dto.getFeatures() != null) {
                motorcycleData.put("features", dto.getFeatures().toString());
            }

            MultipartFile[] imagesArray = (images != null) ? images.toArray(new MultipartFile[0]) : new MultipartFile[0];

            MotorcycleResponseDTO updated = service.updateMotorcycle(id, motorcycleData, imagesArray);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "error", "Motorcycle not found",
                            "message", e.getMessage()
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Failed to update motorcycle",
                            "message", e.getMessage()
                    ));
        }
    }

    // DELETE MOTORCYCLE
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            service.deleteMotorcycle(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "error", "Motorcycle not found",
                            "message", e.getMessage()
                    ));
        }
    }

    // ADDITIONAL ENDPOINTS
    @GetMapping("/latest")
    public ResponseEntity<?> getLatestArrivals() {
        return ResponseEntity.ok(service.getLatestArrivals());
    }

    @GetMapping("/filter-options")
    public ResponseEntity<?> getFilterOptions() {
        return ResponseEntity.ok(service.getFilterOptions());
    }

    @GetMapping("/brands")
    public ResponseEntity<?> getBrandsWithCount() {
        return ResponseEntity.ok(service.getDistinctBrandsWithCount());
    }
}