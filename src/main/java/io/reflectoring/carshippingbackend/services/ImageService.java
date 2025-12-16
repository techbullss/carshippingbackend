package io.reflectoring.carshippingbackend.services;

;
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

    @Transactional
    public Image uploadImage(MultipartFile file) throws IOException {
        log.info("Uploading image: {}", file.getOriginalFilename());

        // Create new image entity
        Image image = new Image();
        image.setId(UUID.randomUUID().toString());
        image.setFileName(generateFileName(file));
        image.setOriginalName(file.getOriginalFilename());
        image.setFileType(file.getContentType());
        image.setFileSize(file.getSize());
        image.setUploadedAt(LocalDateTime.now());

        // For now, store image locally (you can replace with Cloudinary later)
        // In production, upload to Cloudinary/S3 and set the URL
        image.setUrl("/images/" + image.getFileName());

        // Make first image active
        if (imageRepository.count() == 0) {
            image.setActive(true);
        }

        return imageRepository.save(image);
    }

    @Transactional
    public void deleteImage(String id) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found"));

        // If deleting active image, make another one active
        if (image.isActive()) {
            List<Image> images = imageRepository.findAllByOrderByUploadedAtDesc();
            if (images.size() > 1) {
                Image nextImage = images.get(1); // Skip the one being deleted
                nextImage.setActive(true);
                imageRepository.save(nextImage);
            }
        }

        imageRepository.delete(image);
    }

    @Transactional
    public Image rotateImage() {
        List<Image> images = imageRepository.findAllByOrderByUploadedAtDesc();
        if (images.isEmpty()) {
            return null;
        }

        // Find current active image index
        int currentIndex = -1;
        for (int i = 0; i < images.size(); i++) {
            if (images.get(i).isActive()) {
                currentIndex = i;
                break;
            }
        }

        // If no active image or last image, make first active
        if (currentIndex == -1 || currentIndex == images.size() - 1) {
            images.forEach(img -> img.setActive(false));
            Image firstImage = images.get(0);
            firstImage.setActive(true);
            imageRepository.saveAll(images);
            return firstImage;
        }

        // Make next image active
        images.forEach(img -> img.setActive(false));
        Image nextImage = images.get(currentIndex + 1);
        nextImage.setActive(true);
        imageRepository.saveAll(images);

        return nextImage;
    }

    public Image getCurrentImage() {
        return imageRepository.findByActiveTrue()
                .orElse(null);
    }

    public List<Image> getAllImages() {
        return imageRepository.findAllByOrderByUploadedAtDesc();
    }

    public long getImageCount() {
        return imageRepository.count();
    }

    private String generateFileName(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        return UUID.randomUUID() + extension;
    }
}