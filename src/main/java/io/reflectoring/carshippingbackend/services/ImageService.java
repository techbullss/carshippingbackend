package io.reflectoring.carshippingbackend.services;

import io.reflectoring.carshippingbackend.DTO.ImageDTO;
import io.reflectoring.carshippingbackend.DTO.UploadResponse;
import io.reflectoring.carshippingbackend.repository.ImageRepository;
import io.reflectoring.carshippingbackend.tables.Image;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {
    private final ImageRepository imageRepository;
    private final CloudStorageService cloudStorageService;  // Changed from CloudinaryService
    private final ImageRotationService rotationService;

    @Transactional
    public UploadResponse uploadImage(MultipartFile file) throws IOException {
        log.info("Starting image upload: {}", file.getOriginalFilename());

        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (!file.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }

        // Upload to Cloudinary using CloudStorageService
        String imageUrl = cloudStorageService.uploadFile(file, "car-shipping/images");
        log.info("Image uploaded to Cloudinary: {}", imageUrl);

        // Extract public ID from URL
        String publicId = extractPublicIdFromUrl(imageUrl);

        // Save to database
        Image image = new Image();
        image.setId(UUID.randomUUID().toString());
        image.setFileName(generateFileName(file));
        image.setOriginalName(file.getOriginalFilename());
        image.setUrl(imageUrl);
        image.setCloudinaryPublicId(publicId);
        image.setFileType(file.getContentType());
        image.setFileSize(file.getSize());
        image.setUploadedAt(LocalDateTime.now());
        image.setActive(false);

        imageRepository.save(image);
        log.info("Image saved to database: {}", image.getId());

        // Convert to DTO using rotation service
        //ImageDTO imageDto = rotationService.convertToDTO(image);

        // Create response


        log.info("Upload completed successfully");
        return null;
    }

    @Transactional
    public void deleteImage(String id) throws IOException {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Image not found"));

        // Delete from Cloudinary using CloudStorageService
        if (image.getCloudinaryPublicId() != null) {
            // Extract folder and filename from publicId
            String[] parts = image.getCloudinaryPublicId().split("/");
            if (parts.length >= 2) {
                String folderName = parts[0];
                String fileName = parts[1];
                cloudStorageService.deleteFile(folderName, fileName);
            }
        }

        // Delete from database
        imageRepository.delete(image);

        // If this was the active image, rotate to next one
        if (image.isActive()) {
            rotationService.rotateToNextImage();
        }
    }

    private String generateFileName(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf(".");
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex);
        }
        return UUID.randomUUID().toString() + extension;
    }

    /**
     * Extract public ID from Cloudinary URL
     * Example URL: https://res.cloudinary.com/demo/image/upload/v1234567890/car-shipping/images/filename.jpg
     * Returns: car-shipping/images/filename
     */
    private String extractPublicIdFromUrl(String url) {
        try {
            // Find the part after "/upload/"
            int uploadIndex = url.indexOf("/upload/");
            if (uploadIndex == -1) {
                return null;
            }

            // Get everything after "/upload/"
            String path = url.substring(uploadIndex + 8);

            // Remove version prefix if present (v1234567890/)
            if (path.startsWith("v")) {
                int slashIndex = path.indexOf("/");
                if (slashIndex != -1) {
                    path = path.substring(slashIndex + 1);
                }
            }

            // Remove file extension
            int lastDotIndex = path.lastIndexOf(".");
            if (lastDotIndex != -1) {
                path = path.substring(0, lastDotIndex);
            }

            return path;

        } catch (Exception e) {
            log.error("Failed to extract public ID from URL: {}", url, e);
            return null;
        }
    }

    public List<ImageDTO> getAllImages() {
        return rotationService.getAllImages();
    }

    public ImageDTO getImageById(String id) {
        return imageRepository.findById(id)
                .map(rotationService::convertToDTO)
                .orElseThrow(() -> new IllegalArgumentException("Image not found"));
    }

    public long getImageCount() {
        return imageRepository.count();
    }
}