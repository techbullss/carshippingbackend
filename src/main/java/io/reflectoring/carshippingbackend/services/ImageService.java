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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service

@RequiredArgsConstructor
public class ImageService {
    private final ImageRepository imageRepository;
    private final CloudinaryService cloudinaryService;
    private final ImageRotationService rotationService;

    @Transactional
    public UploadResponse uploadImage(MultipartFile file) throws IOException {
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (!file.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }

        // Upload to Cloudinary
        String imageUrl = cloudinaryService.uploadImage(file);
        String publicId = cloudinaryService.extractPublicIdFromUrl(imageUrl);

        // Save to database
        Image image = new Image();
        image.setId(UUID.randomUUID().toString());
        image.setFileName(generateFileName(file));
        image.setOriginalName(file.getOriginalFilename());
        image.setUrl(imageUrl);
        image.setCloudinaryPublicId(publicId);
        image.setFileType(file.getContentType());
        image.setFileSize(file.getSize());

        imageRepository.save(image);


        return new UploadResponse(true, "Image uploaded successfully",
                rotationService.convertToDTO(image));
    }

    @Transactional
    public void deleteImage(String id) throws IOException {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Image not found"));

        // Delete from Cloudinary
        if (image.getCloudinaryPublicId() != null) {
            cloudinaryService.deleteImage(image.getCloudinaryPublicId());
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
        return UUID.randomUUID().toString() +
                originalFilename.substring(originalFilename.lastIndexOf("."));
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
