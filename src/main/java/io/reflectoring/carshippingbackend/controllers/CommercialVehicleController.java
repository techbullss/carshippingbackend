package io.reflectoring.carshippingbackend.controllers;

import io.reflectoring.carshippingbackend.DTO.CommercialVehicleDTO;
import io.reflectoring.carshippingbackend.DTO.CommercialVehicleResponseDTO;
import io.reflectoring.carshippingbackend.services.CommercialVehicleService;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@CrossOrigin(origins = "https://carshippingfrontend.vercel.app/")
@RequiredArgsConstructor
public class CommercialVehicleController {

    private final CommercialVehicleService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommercialVehicleResponseDTO> create(
            @RequestPart("vehicle") CommercialVehicleDTO dto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) throws IOException {
        dto.setImages(images); //
        return ResponseEntity.ok(service.createVehicle(dto));
    }

    @GetMapping
    public ResponseEntity<Page<CommercialVehicleResponseDTO>> getAllVehicles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type
    ) {
        return ResponseEntity.ok(service.searchVehicles(page, size, search, type));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommercialVehicleResponseDTO> getVehicle(@PathVariable Long id) {
        return ResponseEntity.ok(service.getVehicle(id));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommercialVehicleResponseDTO> update(
            @PathVariable Long id,
            @RequestPart("vehicle") CommercialVehicleDTO dto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) throws IOException {
        dto.setImages(images);
        return ResponseEntity.ok(service.updateVehicle(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }
}
