package io.reflectoring.carshippingbackend.controllers;
import io.reflectoring.carshippingbackend.services.ImageService;
import io.reflectoring.carshippingbackend.tables.Image;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
@CrossOrigin(origins = {"https://www.f-carshipping.com", "https://f-carshipping.com", "http://localhost:3000"})
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    // Get all images
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllImages() {
        try {
            List<Image> images = imageService.getAllImages();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("images", images);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Failed to fetch images"));
        }
    }

    // Get current image
    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentImage() {
        try {
            Image currentImage = imageService.getCurrentImage();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);

            if (currentImage != null) {
                response.put("image", currentImage);
                // Calculate next rotation (48 hours from upload time)
                LocalDateTime nextRotation = currentImage.getUploadedAt().plusHours(48);
                response.put("nextRotation", nextRotation.toString());
            } else {
                response.put("image", null);
                response.put("nextRotation", null);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Failed to get current image"));
        }
    }

    // Upload image
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadImage(@RequestParam("image") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "error", "File is empty"));
            }

            if (!file.getContentType().startsWith("image/")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "error", "File must be an image"));
            }

            Image uploadedImage = imageService.uploadImage(file);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Image uploaded successfully");
            response.put("image", "");

            return ResponseEntity.ok(response);
        } catch (Error e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Failed to upload image"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Delete image
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteImage(@PathVariable long id) {
        try {
            imageService.deleteImage(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Image deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "error", "Image not found"));
        }
    }

    // Force rotate image
    @PostMapping("/rotate")
    public ResponseEntity<Map<String, Object>> rotateImage() {
        try {
            Image rotatedImage = imageService.rotateImage();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Image rotated successfully");

            if (rotatedImage != null) {
                response.put("image", rotatedImage);
                LocalDateTime nextRotation = rotatedImage.getUploadedAt().plusHours(48);
                response.put("nextRotation", nextRotation.toString());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Failed to rotate image"));
        }
    }

    // Get image stats
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalImages", imageService.getImageCount());
            stats.put("rotationIntervalHours", 48);

            Image currentImage = imageService.getCurrentImage();
            if (currentImage != null) {
                stats.put("currentImage", currentImage.getOriginalName());
                stats.put("nextRotation", currentImage.getUploadedAt().plusHours(48).toString());
            }

            return ResponseEntity.ok(Map.of("success", true, "data", stats));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Failed to get stats"));
        }
    }

    // Health check
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "image-rotation-service"));
    }
}