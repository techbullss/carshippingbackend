package io.reflectoring.carshippingbackend.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CloudStorageService {

    private final Cloudinary cloudinary;

    /**
     * Upload a file (e.g., govt ID or passport photo) to Cloudinary.
     *
     * @param file       the MultipartFile to upload
     * @param folderName Cloudinary folder name (e.g. "user-ids")
     * @return the secure URL of the uploaded file
     */
    public String uploadFile(MultipartFile file, String folderName) {
        try {
            if (file == null || file.isEmpty()) {
                throw new RuntimeException("No file provided for upload");
            }

            // Create a unique filename to prevent collisions
            String uniqueFileName = UUID.randomUUID() + "-" +
                    Objects.requireNonNull(file.getOriginalFilename()).replaceAll("\\s+", "_");

            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", folderName + "/" + uniqueFileName,
                            "resource_type", "auto"
                    )
            );

            // Return the hosted file URL
            return (String) uploadResult.get("secure_url");

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file: " + e.getMessage());
        }
    }

    /**
     * Deletes a file from Cloudinary using its public ID.
     * You should store or extract the public_id when saving the user.
     *
     * @param folderName the folder (e.g., "user-ids")
     * @param fileName   the file name used when uploading (without extension)
     */
    public void deleteFile(String folderName, String fileName) {
        try {
            String publicId = folderName + "/" + fileName;
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + e.getMessage());
        }
    }
}
