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
}

