package io.reflectoring.carshippingbackend.controllers;

import io.reflectoring.carshippingbackend.DTO.ImageDTO;
import io.reflectoring.carshippingbackend.DTO.RotationResponse;
import io.reflectoring.carshippingbackend.services.ImageRotationService;
import io.reflectoring.carshippingbackend.services.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
@CrossOrigin(origins = {
        "https://www.f-carshipping.com",
        "https://f-carshipping.com",
        "http://localhost:3000"
})
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;                 // Upload/Delete
    private final ImageRotationService imageRotationService; // 48h Rotation Logic

    /* =========================================================
       GET ALL IMAGES
       ========================================================= */
    @GetMapping
    public ResponseEntity<?> getAllImages() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "images", imageRotationService.getAllImages()
        ));
    }

    /* =========================================================
       GET CURRENT IMAGE (AUTO ROTATES IF 48H PASSED)
       ========================================================= */
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentImage() {

        RotationResponse rotation = imageRotationService.getCurrentImage();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "image", rotation.getCurrentImage(),
                "nextRotation", rotation.getNextRotation(),
                "totalImages", rotation.getTotalImages(),
                "currentIndex", rotation.getCurrentIndex()
        ));
    }

    /* =========================================================
       UPLOAD IMAGE
       ========================================================= */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImage(@RequestParam("image") MultipartFile file) {
        try {

            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "error", "File is empty"));
            }

            if (file.getContentType() == null ||
                    !file.getContentType().startsWith("image/")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "error", "File must be an image"));
            }

            imageService.uploadImage(file);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Image uploaded successfully"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Upload failed"));
        }
    }

    /* =========================================================
       DELETE IMAGE
       ========================================================= */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteImage(@PathVariable long id) {
        try {
            imageService.deleteImage(id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Image deleted successfully"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "error", "Image not found"));
        }
    }

    /* =========================================================
       FORCE ROTATE (ADMIN TESTING)
       ========================================================= */
    @PostMapping("/rotate")
    public ResponseEntity<?> forceRotate() {

        imageRotationService.forceRotate();

        RotationResponse rotation = imageRotationService.getCurrentImage();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Image rotated successfully",
                "image", rotation.getCurrentImage(),
                "nextRotation", rotation.getNextRotation()
        ));
    }

    /* =========================================================
       STATS
       ========================================================= */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {

        RotationResponse rotation = imageRotationService.getCurrentImage();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "totalImages", rotation.getTotalImages(),
                "currentIndex", rotation.getCurrentIndex(),
                "nextRotation", rotation.getNextRotation(),
                "rotationIntervalHours", 48
        ));
    }

    /* =========================================================
       HEALTH CHECK
       ========================================================= */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "image-rotation-service"
        ));
    }
}