package io.reflectoring.carshippingbackend.services;

import io.reflectoring.carshippingbackend.DTO.ImageDTO;
import io.reflectoring.carshippingbackend.DTO.RotationResponse;
import io.reflectoring.carshippingbackend.repository.ImageRepository;
import io.reflectoring.carshippingbackend.repository.RotationConfigRepository;
import io.reflectoring.carshippingbackend.tables.Image;
import io.reflectoring.carshippingbackend.tables.RotationConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional  // ADD THIS AT CLASS LEVEL
public class ImageRotationService {
    private final ImageRepository imageRepository;
    private final RotationConfigRepository configRepository;

    private static final int DEFAULT_ROTATION_INTERVAL_HOURS = 48;

    // Get or create configuration value
    private String getConfigValue(String key, String defaultValue) {
        return configRepository.findByConfigKey(key)
                .map(RotationConfig::getConfigValue)
                .orElseGet(() -> {
                    RotationConfig config = new RotationConfig();
                    config.setConfigKey(key);
                    config.setConfigValue(defaultValue);
                    configRepository.save(config);
                    return defaultValue;
                });
    }

    private void setConfigValue(String key, String value) {
        RotationConfig config = configRepository.findByConfigKey(key)
                .orElse(new RotationConfig());
        config.setConfigKey(key);
        config.setConfigValue(value);
        configRepository.save(config);
    }

    public RotationResponse getCurrentImage() {
        // Check if rotation is needed
        checkAndPerformRotation();

        // Get current index
        int currentIndex = Integer.parseInt(
                getConfigValue(RotationConfig.CURRENT_IMAGE_INDEX, "0")
        );

        // Get all images ordered by upload date
        List<Image> allImages = imageRepository.findAllByOrderByUploadedAtDesc();

        if (allImages.isEmpty()) {
            return new RotationResponse(null, null, 0, 0);
        }

        // Ensure index is within bounds
        if (currentIndex >= allImages.size()) {
            currentIndex = 0;
            setConfigValue(RotationConfig.CURRENT_IMAGE_INDEX, "0");
        }

        // Get current image
        Image currentImage = allImages.get(currentIndex);

        // Update active status
        updateActiveStatus(currentImage.getId());

        // Calculate next rotation time
        String lastRotationStr = getConfigValue(RotationConfig.LAST_ROTATION_TIME,
                LocalDateTime.now().toString());
        LocalDateTime lastRotation = LocalDateTime.parse(lastRotationStr);

        int intervalHours = Integer.parseInt(
                getConfigValue(RotationConfig.ROTATION_INTERVAL_HOURS,
                        String.valueOf(DEFAULT_ROTATION_INTERVAL_HOURS))
        );

        LocalDateTime nextRotation = lastRotation.plusHours(intervalHours);

        return new RotationResponse(
                convertToDTO(currentImage),
                nextRotation,
                allImages.size(),
                currentIndex
        );
    }

    public void checkAndPerformRotation() {
        String lastRotationStr = getConfigValue(RotationConfig.LAST_ROTATION_TIME,
                LocalDateTime.now().toString());
        LocalDateTime lastRotation = LocalDateTime.parse(lastRotationStr);

        int intervalHours = Integer.parseInt(
                getConfigValue(RotationConfig.ROTATION_INTERVAL_HOURS,
                        String.valueOf(DEFAULT_ROTATION_INTERVAL_HOURS))
        );

        LocalDateTime now = LocalDateTime.now();
        long hoursSinceRotation = ChronoUnit.HOURS.between(lastRotation, now);

        if (hoursSinceRotation >= intervalHours) {
            rotateToNextImage();
            setConfigValue(RotationConfig.LAST_ROTATION_TIME, now.toString());
        }
    }

    public void rotateToNextImage() {
        List<Image> allImages = imageRepository.findAllByOrderByUploadedAtDesc();

        if (allImages.isEmpty()) {
            return;
        }

        int currentIndex = Integer.parseInt(
                getConfigValue(RotationConfig.CURRENT_IMAGE_INDEX, "0")
        );

        // Move to next image, wrap around if at the end
        currentIndex = (currentIndex + 1) % allImages.size();

        setConfigValue(RotationConfig.CURRENT_IMAGE_INDEX, String.valueOf(currentIndex));
        setConfigValue(RotationConfig.LAST_ROTATION_TIME, LocalDateTime.now().toString());

        // Update active status
        updateActiveStatus(allImages.get(currentIndex).getId());
    }

    public void forceRotate() {
        rotateToNextImage();
    }

    public void updateActiveStatus(String activeImageId) {
        // Deactivate all images
        List<Image> allImages = imageRepository.findAll();
        allImages.forEach(img -> img.setActive(false));
        imageRepository.saveAll(allImages);

        // Activate current image
        imageRepository.findById(activeImageId).ifPresent(img -> {
            img.setActive(true);
            imageRepository.save(img);
        });
    }

    // Convert entity to DTO
    public ImageDTO convertToDTO(Image image) {
        if (image == null) return null;

        ImageDTO dto = new ImageDTO();
        dto.setId(image.getId());
        dto.setFileName(image.getFileName());
        dto.setOriginalName(image.getOriginalName());
        dto.setUrl(image.getUrl());
        dto.setFileType(image.getFileType());
        dto.setFileSize(image.getFileSize());
        dto.setUploadedAt(image.getUploadedAt());
        dto.setActive(image.isActive());

        // Format file size
        if (image.getFileSize() != null) {
            dto.setFormattedSize(formatFileSize(image.getFileSize()));
        }

        // Format date
        if (image.getUploadedAt() != null) {
            dto.setUploadDateFormatted(image.getUploadedAt().toString());
        }

        return dto;
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "B";
        return String.format("%.1f %s", bytes / Math.pow(1024, exp), pre);
    }

    public List<ImageDTO> getAllImages() {
        return imageRepository.findAllByOrderByUploadedAtDesc()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
}