package io.reflectoring.carshippingbackend.controllers;


import io.reflectoring.carshippingbackend.DTO.MotorcycleRequestDTO;
import io.reflectoring.carshippingbackend.DTO.MotorcycleResponseDTO;
import io.reflectoring.carshippingbackend.services.MotorcycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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



        String userEmail = "bwanamaina2010@gmail.com";
        String userRole = "ROLE_ADMIN";

        System.out.println("User: " + userEmail + ", Role: " + userRole);

        // Set images from request part
        dto.setImages(images);

        // Override owner with authenticated user's email
        dto.setOwner(userEmail);
        dto.setSeller("PENDING");

        // Ensure status is set


        try {
            MotorcycleResponseDTO created = service.createMotorcycle(dto, userEmail);
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

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status
    ) {
        Page<MotorcycleResponseDTO> p = service.search(page, size, search, type, status);
        return ResponseEntity.ok(p);
    }

    @GetMapping("/filter")
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(required = false) String make,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String priceRange,
            @RequestParam(required = false) String year
    ) {
        Page<MotorcycleResponseDTO> p = service.filterMotorcycles(page, size, make, type, priceRange, year);
        return ResponseEntity.ok("v");
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
}
