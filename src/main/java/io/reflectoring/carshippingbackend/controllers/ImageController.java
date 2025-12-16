package io.reflectoring.carshippingbackend.controllers;

import io.reflectoring.carshippingbackend.DTO.ApiResponse;
import io.reflectoring.carshippingbackend.DTO.ImageDTO;
import io.reflectoring.carshippingbackend.DTO.RotationResponse;
import io.reflectoring.carshippingbackend.DTO.UploadResponse;
import io.reflectoring.carshippingbackend.services.ImageRotationService;
import io.reflectoring.carshippingbackend.services.ImageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Slf4j
@Validated
@CrossOrigin(origins = "https://f-carshipping.com") // Adjust for production
public class ImageController {
    private final ImageService imageService;
    private final ImageRotationService rotationService;

    // Get all images
    @GetMapping
    public ResponseEntity<ApiResponse<List<ImageDTO>>> getAllImages() {
        try {
            List<ImageDTO> images = imageService.getAllImages();
            return ResponseEntity.ok(ApiResponse.success(images));
        } catch (Exception e) {
            log.error("Error fetching images: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch images"));
        }
    }

    // Get current image with rotation logic
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<RotationResponse>> getCurrentImage() {
        try {
            RotationResponse response = rotationService.getCurrentImage();
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Error getting current image: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get current image"));
        }
    }

    // Upload image
    @PostMapping( consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UploadResponse>> uploadImage(
            @RequestPart("image") MultipartFile file) {
        try {
            UploadResponse response = imageService.uploadImage(file);
            return ResponseEntity.ok(ApiResponse.success("Image uploaded successfully", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IOException e) {
            log.error("Error uploading image: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to upload image"));
        }
    }

    // Delete image
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteImage(@PathVariable String id) {
        try {
            imageService.deleteImage(id);
            return ResponseEntity.ok(ApiResponse.success("Image deleted successfully", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IOException e) {
            log.error("Error deleting image: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete image"));
        }
    }

    // Force rotate to next image (for testing)
    @PostMapping("/rotate")
    public ResponseEntity<ApiResponse<RotationResponse>> forceRotate() {
        try {
            rotationService.forceRotate();
            RotationResponse response = rotationService.getCurrentImage();
            return ResponseEntity.ok(ApiResponse.success("Image rotated successfully", response));
        } catch (Exception e) {
            log.error("Error rotating image: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to rotate image"));
        }
    }

    // Get image stats
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalImages", imageService.getImageCount());
            stats.put("rotationIntervalHours", 48);

            RotationResponse current = rotationService.getCurrentImage();
            stats.put("currentImage", current.getCurrentImage());
            stats.put("nextRotation", current.getNextRotation());

            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("Error getting stats: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get stats"));
        }
    }

    // Get single image by ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ImageDTO>> getImageById(@PathVariable String id) {
        try {
            ImageDTO image = imageService.getImageById(id);
            return ResponseEntity.ok(ApiResponse.success(image));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // Health check
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "image-rotation-service");
        return ResponseEntity.ok(response);
    }
}