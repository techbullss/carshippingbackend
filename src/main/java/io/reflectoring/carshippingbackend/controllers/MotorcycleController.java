package io.reflectoring.carshippingbackend.controllers;


import io.reflectoring.carshippingbackend.DTO.MotorcycleRequestDTO;
import io.reflectoring.carshippingbackend.DTO.MotorcycleResponseDTO;
import io.reflectoring.carshippingbackend.services.MotorcycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/motorcycles")
@CrossOrigin(origins = "https://f-carshipping.com/")
@RequiredArgsConstructor
public class MotorcycleController {

    private final MotorcycleService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MotorcycleResponseDTO> create(
            @RequestPart("motorcycle") MotorcycleRequestDTO dto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) throws IOException {
        dto.setImages(images);
        return ResponseEntity.ok(service.create(dto));
    }

    @GetMapping
    public ResponseEntity<Page<MotorcycleResponseDTO>> list(
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
    public ResponseEntity<Page<MotorcycleResponseDTO>> list(
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
    @GetMapping("/{id}")
    public ResponseEntity<MotorcycleResponseDTO> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(service.getOne(id));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MotorcycleResponseDTO> update(
            @PathVariable Long id,
            @RequestPart("motorcycle") MotorcycleRequestDTO dto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) throws IOException {
        dto.setImages(images);
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
