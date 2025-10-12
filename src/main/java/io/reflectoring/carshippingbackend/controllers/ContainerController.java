package io.reflectoring.carshippingbackend.controllers;
import io.reflectoring.carshippingbackend.DTO.ContainerDTO;
import io.reflectoring.carshippingbackend.DTO.ContainerResponseDTO;
import io.reflectoring.carshippingbackend.services.ContainerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/containers")
@CrossOrigin(origins = "https://carshippingfrontend.vercel.app/")
@RequiredArgsConstructor
public class ContainerController {

    private final ContainerService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContainerResponseDTO> create(
            @ModelAttribute ContainerDTO dto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) throws IOException {
        dto.setImages(images);
        return ResponseEntity.ok(service.saveContainer(dto));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContainerResponseDTO> update(
            @PathVariable Long id,
            @ModelAttribute ContainerDTO dto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) throws IOException {
        dto.setImages(images);
        return ResponseEntity.ok(service.updateContainer(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContainerResponseDTO> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(service.getContainer(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteContainer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<ContainerResponseDTO>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(service.searchContainers(page, size, search, status));
    }
}

