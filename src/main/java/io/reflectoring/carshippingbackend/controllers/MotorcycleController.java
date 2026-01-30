package io.reflectoring.carshippingbackend.controllers;

import io.reflectoring.carshippingbackend.DTO.MotorcycleRequestDTO;
import io.reflectoring.carshippingbackend.DTO.MotorcycleResponseDTO;
import io.reflectoring.carshippingbackend.services.MotorcycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
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
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) throws IOException {

        System.out.println("=== CREATE MOTORCYCLE WITH DTO ===");
        System.out.println("DTO received:");
        System.out.println("  Brand: " + dto.getBrand());
        System.out.println("  Model: " + dto.getModel());
        System.out.println("  Type: " + dto.getType());
        System.out.println("  Price: " + dto.getPrice());
        System.out.println("  Year: " + dto.getYear());
        System.out.println("  Features: " + dto.getFeatures());
        System.out.println("  Images param count: " + (images != null ? images.size() : 0));

        // Set images from request part
        dto.setImages(images);
        // Set initial status to PENDING
        dto.setStatus("PENDING");

        try {
            MotorcycleResponseDTO created = service.createMotorcycle(dto);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            System.err.println("Error creating motorcycle: " + e.getMessage());
            e.printStackTrace();
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
        // For public website, always show only APPROVED vehicles
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
        // Default to APPROVED if no status provided
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
        // Get user info from authentication
        String currentUserEmail = authentication.getName();
        String currentUserRole = authentication.getAuthorities().iterator().next().getAuthority();

        // Build filters
        Map<String, String> filters = new HashMap<>();
        if (search != null && !search.isBlank()) filters.put("search", search);
        if (type != null && !type.isBlank()) filters.put("type", type);
        if (status != null && !status.isBlank()) filters.put("status", status);
        if (brand != null && !brand.isBlank()) filters.put("brand", brand);
        if (owner != null && !owner.isBlank()) filters.put("owner", owner);

        // Use role-based search
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
        // Only show approved vehicles in filter
        Page<MotorcycleResponseDTO> p = service.filterMotorcycles(page, size, make, type, priceRange, year);
        return ResponseEntity.ok(p);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id) {
        return ResponseEntity.ok("service.getOne(id)");
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestPart("motorcycle") MotorcycleRequestDTO dto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) throws IOException {
        dto.setImages(images);
        return ResponseEntity.ok("service.update(id, dto)");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        // service.delete(id);
        return ResponseEntity.noContent().build();
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