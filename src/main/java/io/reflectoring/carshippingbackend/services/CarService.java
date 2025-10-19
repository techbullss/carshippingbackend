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
System.out.println("sereee"+car.getSeller());
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
        // 1️⃣ Fetch the existing car from the DB
        Optional<Car> existingOpt = repo.findById(car.getId());
        if (existingOpt.isEmpty()) {
            throw new RuntimeException("Car not found with ID: " + car.getId());
        }
        Car existing = existingOpt.get();

        // 2️⃣ Update all fields
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

        // 3️⃣ Handle new images (if any)
        if (images != null && images.length > 0) {
            List<String> urls = existing.getImageUrls() != null ? new ArrayList<>(existing.getImageUrls()) : new ArrayList<>();

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

                urls.add((String) uploadResult.get("secure_url"));
            }

            existing.setImageUrls(urls);
        }

        // 4️⃣ Save updated car
        return repo.save(existing);
    }
    public Page<Car> searchByUserRole(Map<String, String> allParams, int page, int size, Sort sort, String email, String role) {
        Pageable pageable = PageRequest.of(page, size, sort);

        if ("ADMIN".equalsIgnoreCase(role)) {
            return repo.search(allParams, pageable); // all cars
        } else {
            return repo.searchBySeller(allParams, pageable, email); // only seller's cars
        }
    }

}

