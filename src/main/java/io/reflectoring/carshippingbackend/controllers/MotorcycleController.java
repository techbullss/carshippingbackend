package io.reflectoring.carshippingbackend.controllers;

import io.reflectoring.carshippingbackend.DTO.MotorcycleRequestDTO;
import io.reflectoring.carshippingbackend.DTO.MotorcycleResponseDTO;
import io.reflectoring.carshippingbackend.services.MotorcycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/motorcycles")
@CrossOrigin(origins = "https://f-carshipping.com")
@RequiredArgsConstructor
public class MotorcycleController {

    private final MotorcycleService service;

    // ============= CREATE =============
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
            @RequestPart("motorcycle") MotorcycleRequestDTO dto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            Authentication authentication
    ) throws IOException {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Authentication required");
        }

        String userEmail = authentication.getName();
        dto.setImages(images);
        dto.setStatus("PENDING");
        dto.setOwner(userEmail);

        MotorcycleResponseDTO created = service.createMotorcycle(dto, userEmail);
        return ResponseEntity.ok(created);
    }

    // ============= MAIN SEARCH ENDPOINT =============
    @GetMapping
    public ResponseEntity<Page<MotorcycleResponseDTO>> search(
            @RequestParam Map<String, String> allParams,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            Authentication authentication
    ) {
        // Parse sort parameter
        String[] sortParts = sort.split(",");
        org.springframework.data.domain.Sort s = org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.fromString(
                        sortParts.length > 1 ? sortParts[1] : "desc"),
                sortParts[0]);

        // Extract filters (remove pagination/sorting params)
        Map<String, String> filters = allParams.entrySet().stream()
                .filter(entry -> !entry.getKey().equals("page") &&
                        !entry.getKey().equals("size") &&
                        !entry.getKey().equals("sort"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // Determine user role and email
        String userEmail = authentication != null ? authentication.getName() : null;
        String userRole = authentication != null ?
                authentication.getAuthorities().iterator().next().getAuthority() : "PUBLIC";

        Page<MotorcycleResponseDTO> results = service.searchByUserRole(
                filters, page, size, s, userEmail, userRole);

        return ResponseEntity.ok(results);
    }

    // ============= LEGACY FILTER ENDPOINT (for backward compatibility) =============
    @GetMapping("/filter")
    public ResponseEntity<Page<MotorcycleResponseDTO>> filter(
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

    // ============= GET BY ID =============
    @GetMapping("/{id}")
    public ResponseEntity<MotorcycleResponseDTO> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(service.getMotorcycle(id));
    }

    // ============= UPDATE =============
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MotorcycleResponseDTO> update(
            @PathVariable Long id,
            @RequestPart("motorcycle") MotorcycleRequestDTO dto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            Authentication authentication
    ) throws IOException {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        dto.setImages(images);
        MotorcycleResponseDTO updated = service.updateMotorcycle(id, dto);
        return ResponseEntity.ok(updated);
    }

    // ============= DELETE =============
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        service.deleteMotorcycle(id);
        return ResponseEntity.noContent().build();
    }

    // ============= APPROVE (Admin only) =============
    @PutMapping("/approve/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MotorcycleResponseDTO> approve(@PathVariable Long id) {
        return ResponseEntity.ok(service.approveMotorcycle(id));
    }

    // ============= REJECT (Admin only) =============
    @PutMapping("/reject/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MotorcycleResponseDTO> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "No reason provided");
        return ResponseEntity.ok(service.rejectMotorcycle(id, reason));
    }

    // ============= LATEST ARRIVALS =============
    @GetMapping("/latest")
    public ResponseEntity<List<MotorcycleResponseDTO>> latest() {
        return ResponseEntity.ok(service.getLatestArrivals());
    }

    // ============= FILTER OPTIONS =============
    @GetMapping("/filters/options")
    public ResponseEntity<Map<String, List<String>>> getFilterOptions() {
        return ResponseEntity.ok(service.getFilterOptions());
    }
}