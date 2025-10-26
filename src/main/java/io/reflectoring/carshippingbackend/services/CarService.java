package io.reflectoring.carshippingbackend.services;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import io.reflectoring.carshippingbackend.repository.CarRepository;
import io.reflectoring.carshippingbackend.tables.Car;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CarService {
    private final CarRepository repo;
    private final Cloudinary cloudinary;

    private String uploadDir;

    public CarService(CarRepository repo, Cloudinary cloudinary) { this.repo = repo;
        this.cloudinary = cloudinary;
    }

    public Page<Car> search(Map<String, String> params, int page, int size, Sort sort) {
        var spec = CarSpecification.byFilters(params);
        Pageable pageable = PageRequest.of(page, size, sort);
        return repo.findAll(spec, pageable);
    }

    public Car create(Car car, MultipartFile[] images) throws IOException {
        if (images != null && images.length > 0) {
            List<String> urls = new ArrayList<>();

            for (MultipartFile f : images) {
                String uniqueFileName = UUID.randomUUID() + "-" +
                        Objects.requireNonNull(f.getOriginalFilename()).replaceAll("\\s+", "_");

                Map uploadResult = cloudinary.uploader().upload(
                        f.getBytes(),
                        ObjectUtils.asMap(
                                "public_id", "uploads/" + uniqueFileName,
                                "resource_type", "auto" // handles all types
                        )
                );

                urls.add((String) uploadResult.get("secure_url"));
            }

        car.setImageUrls(urls);
        }

        return repo.save(car);
    }
    public Car update(Car car, MultipartFile[] images) throws IOException {
        // 1️⃣ Fetch existing car
        Car existing = repo.findById(car.getId())
                .orElseThrow(() -> new RuntimeException("Car not found with ID: " + car.getId()));

        // 2️⃣ Update fields
        existing.setBrand(car.getBrand());
        existing.setModel(car.getModel());
        existing.setYearOfManufacture(car.getYearOfManufacture());
        existing.setConditionType(car.getConditionType());
        existing.setBodyType(car.getBodyType());
        existing.setColor(car.getColor());
        existing.setEngineType(car.getEngineType());
        existing.setEngineCapacityCc(car.getEngineCapacityCc());
        existing.setFuelType(car.getFuelType());
        existing.setTransmission(car.getTransmission());
        existing.setSeats(car.getSeats());
        existing.setDoors(car.getDoors());
        existing.setMileageKm(car.getMileageKm());
        existing.setPriceKes(car.getPriceKes());
        existing.setDescription(car.getDescription());
        existing.setLocation(car.getLocation());
        existing.setOwnerType(car.getOwnerType());
        existing.setFeatures(car.getFeatures());
        existing.setCustomSpecs(car.getCustomSpecs());

        // 3️⃣ Prepare image URLs
        List<String> updatedUrls = new ArrayList<>();

        // 3a️⃣ Keep URLs that were sent from frontend (still active)
        if (car.getImageUrls() != null && !car.getImageUrls().isEmpty()) {
            updatedUrls.addAll(car.getImageUrls());
        }

        // 4️⃣ Delete old images from Cloudinary that are no longer kept
        if (existing.getImageUrls() != null) {
            List<String> removedUrls = existing.getImageUrls().stream()
                    .filter(url -> car.getImageUrls() == null || !car.getImageUrls().contains(url))
                    .collect(Collectors.toList());

            for (String url : removedUrls) {
                try {
                    // Extract Cloudinary public_id (after "/uploads/")
                    String publicId = extractPublicId(url);
                    if (publicId != null) {
                        cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
                        System.out.println("🗑️ Deleted from Cloudinary: " + publicId);
                    }
                } catch (Exception ex) {
                    System.err.println("⚠️ Failed to delete Cloudinary image: " + ex.getMessage());
                }
            }
        }

        // 5️⃣ Upload new images
        if (images != null && images.length > 0) {
            for (MultipartFile f : images) {
                String uniqueFileName = UUID.randomUUID() + "-" +
                        Objects.requireNonNull(f.getOriginalFilename()).replaceAll("\\s+", "_");

                Map uploadResult = cloudinary.uploader().upload(
                        f.getBytes(),
                        ObjectUtils.asMap(
                                "public_id", "uploads/" + uniqueFileName,
                                "resource_type", "auto"
                        )
                );

                updatedUrls.add((String) uploadResult.get("secure_url"));
            }
        }

        // 6 Save updated URLs and persist
        existing.setImageUrls(updatedUrls);
        return repo.save(existing);
    }

    /**
     * Helper method to extract Cloudinary public ID from a secure_url.
     * Example: https://res.cloudinary.com/demo/image/upload/v1733344/uploads/abc123.jpg
     * Returns: uploads/abc123
     */
    private String extractPublicId(String url) {
        try {
            int start = url.indexOf("/uploads/");
            if (start == -1) return null;

            String afterUploads = url.substring(start + 1); // remove leading '/'
            String withoutExt = afterUploads.replaceAll("\\.[a-zA-Z0-9]+$", ""); // remove file extension
            return withoutExt; // e.g. "uploads/abc123"
        } catch (Exception e) {
            return null;
        }
    }

    public Page<Car> searchByUserRole(Map<String, String> allParams, int page, int size, Sort sort, String currentUserEmail, String currentUserRole) {
        Pageable pageable = PageRequest.of(page, size, sort);

        switch (currentUserRole.replace("ROLE_", "").toUpperCase()) {
            case "ADMIN":
                return repo.search(allParams, pageable);
            case "SELLER":
                return repo.searchBySeller(allParams, pageable, currentUserEmail);
            default:
                throw new RuntimeException("Unauthorized access");
        }
    }

}

