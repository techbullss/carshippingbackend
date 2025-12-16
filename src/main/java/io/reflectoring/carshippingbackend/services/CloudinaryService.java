package io.reflectoring.carshippingbackend.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class CloudinaryService {
    private final Cloudinary cloudinary;

    public String uploadImage(MultipartFile file) throws IOException {
        String uniqueFileName = UUID.randomUUID() + "-" +
                Objects.requireNonNull(file.getOriginalFilename()).replaceAll("\\s+", "_");

        Map uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "public_id", "rotating-images/" + uniqueFileName,
                        "resource_type", "auto",
                        "folder", "rotating-images"
                )
        );

        String url = (String) uploadResult.get("secure_url");
        String publicId = (String) uploadResult.get("public_id");

        log.info("Image uploaded successfully: {}", url);
        return url;
    }

    public void deleteImage(String publicId) throws IOException {
        try {
            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Image deleted from Cloudinary: {}", result);
        } catch (Exception e) {
            log.error("Error deleting image from Cloudinary: {}", e.getMessage());
            throw e;
        }
    }

    public String extractPublicIdFromUrl(String url) {
        // Extract public_id from Cloudinary URL
        // Example: https://res.cloudinary.com/demo/image/upload/v123/rotating-images/filename.jpg
        try {
            String[] parts = url.split("/upload/");
            if (parts.length > 1) {
                String afterUpload = parts[1];
                // Remove version if present
                if (afterUpload.startsWith("v")) {
                    afterUpload = afterUpload.substring(afterUpload.indexOf("/") + 1);
                }
                // Remove file extension
                int lastDotIndex = afterUpload.lastIndexOf(".");
                if (lastDotIndex > 0) {
                    afterUpload = afterUpload.substring(0, lastDotIndex);
                }
                return afterUpload;
            }
        } catch (Exception e) {
            log.warn("Could not extract public_id from URL: {}", url);
        }
        return null;
    }
}