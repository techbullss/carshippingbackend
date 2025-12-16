package io.reflectoring.carshippingbackend.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
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
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {

    private final ImageRepository imageRepository;
    private final Cloudinary cloudinary;

    @Transactional
    public Image uploadImage(MultipartFile file) throws IOException {
        log.info("Uploading image to Cloudinary: {}", file.getOriginalFilename());

        // Generate Cloudinary public ID
        String publicId = "carousel/" + UUID.randomUUID() + "-" +
                file.getOriginalFilename().replaceAll("\\s+", "_");

        // Upload to Cloudinary
        Map uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "public_id", publicId,
                        "resource_type", "image"
                )
        );

        String secureUrl = (String) uploadResult.get("secure_url");

        // Create image entity
        Image image = new Image();
        image.setFileName(publicId);
        image.setOriginalName(file.getOriginalFilename());
        image.setFileType(file.getContentType());
        image.setFileSize(file.getSize());
        image.setUploadedAt(LocalDateTime.now());
        image.setUrl(secureUrl);

        // Make first image active
        if (imageRepository.count() == 0) {
            image.setActive(true);
        }

        return imageRepository.save(image);
    }

    @Transactional
    public void deleteImage(long id) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found"));

        // Delete from Cloudinary
        try {
            String publicId = extractPublicId(image.getUrl());
            if (publicId != null) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
                log.info("Deleted image from Cloudinary: {}", publicId);
            }
        } catch (Exception e) {
            log.warn("Failed to delete Cloudinary image", e);
        }

        // Handle active image rotation
        if (image.isActive()) {
            List<Image> images = imageRepository.findAllByOrderByUploadedAtDesc();
            if (images.size() > 1) {
                Image next = images.get(1);
                next.setActive(true);
                imageRepository.save(next);
            }
        }

        imageRepository.delete(image);
    }

    @Transactional
    public Image rotateImage() {
        List<Image> images = imageRepository.findAllByOrderByUploadedAtDesc();
        if (images.isEmpty()) return null;

        int activeIndex = -1;
        for (int i = 0; i < images.size(); i++) {
            if (images.get(i).isActive()) {
                activeIndex = i;
                break;
            }
        }

        images.forEach(img -> img.setActive(false));

        Image next;
        if (activeIndex == -1 || activeIndex == images.size() - 1) {
            next = images.get(0);
        } else {
            next = images.get(activeIndex + 1);
        }

        next.setActive(true);
        imageRepository.saveAll(images);

        return next;
    }

    public Image getCurrentImage() {
        return imageRepository.findByActiveTrue().orElse(null);
    }

    public List<Image> getAllImages() {
        return imageRepository.findAllByOrderByUploadedAtDesc();
    }

    public long getImageCount() {
        return imageRepository.count();
    }

    /**
     * Extract Cloudinary public_id from secure_url
     * Example:
     * https://res.cloudinary.com/.../image/upload/v123/carousel/abc.jpg
     * → carousel/abc
     */
    private String extractPublicId(String url) {
        try {
            int start = url.indexOf("/carousel/");
            if (start == -1) return null;

            String after = url.substring(start + 1);
            return after.replaceAll("\\.[a-zA-Z0-9]+$", "");
        } catch (Exception e) {
            return null;
        }
    }
}
