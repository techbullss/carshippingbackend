package io.reflectoring.carshippingbackend.services;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import io.reflectoring.carshippingbackend.DTO.MotorcycleRequestDTO;
import io.reflectoring.carshippingbackend.DTO.MotorcycleResponseDTO;
import io.reflectoring.carshippingbackend.repository.MotorcycleRepository;
import io.reflectoring.carshippingbackend.tables.Motorcycle;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MotorcycleService {

    private final MotorcycleRepository repo;
    private final Cloudinary cloudinary;

    // Upload helper
    private List<String> uploadImages(List<MultipartFile> images) throws IOException {
        if (images == null || images.isEmpty()) return Collections.emptyList();
        List<String> urls = new ArrayList<>();
        for (MultipartFile f : images) {
            String uniqueFileName = UUID.randomUUID() + "-" + Objects.requireNonNull(f.getOriginalFilename()).replaceAll("\\s+", "_");
            Map uploadResult = cloudinary.uploader().upload(f.getBytes(), ObjectUtils.asMap(
                    "public_id", "motorcycles/" + uniqueFileName,
                    "resource_type", "auto"
            ));
            urls.add((String) uploadResult.get("secure_url"));
        }
        return urls;
    }

    // Create
    public MotorcycleResponseDTO create(MotorcycleRequestDTO dto) throws IOException {
        Motorcycle m = Motorcycle.builder()
                .brand(dto.getBrand())
                .model(dto.getModel())
                .type(dto.getType())
                .engineCapacity(dto.getEngineCapacity())
                .status(dto.getStatus())
                .price(dto.getPrice())
                .location(dto.getLocation())
                .owner(dto.getOwner())
                .description(dto.getDescription())
                .features(dto.getFeatures())
                .build();

        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            m.setImageUrls(uploadImages(dto.getImages()));
        }

        Motorcycle saved = repo.save(m);
        return toDto(saved);
    }

    // Read pageable with search + filters
    public Page<MotorcycleResponseDTO> search(int page, int size, String search, String type, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Motorcycle> result;

        if (search != null && !search.isBlank()) {
            result = repo.findByBrandContainingIgnoreCaseOrModelContainingIgnoreCase(search, search, pageable);
        } else if (type != null && !type.isBlank()) {
            result = repo.findByTypeIgnoreCase(type, pageable);
        } else if (status != null && !status.isBlank()) {
            result = repo.findByStatusIgnoreCase(status, pageable);
        } else {
            result = repo.findAll(pageable);
        }

        return result.map(this::toDto);
    }
    public Page<MotorcycleResponseDTO> filterMotorcycles(
            int page,
            int size,
            String make,
            String type,
            String priceRange,
            String year
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        Specification<Motorcycle> spec = Specification.where(null);

        if (make != null && !make.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(cb.lower(root.get("brand")), make.toLowerCase()));
        }

        if (type != null && !type.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(cb.lower(root.get("type")), type.toLowerCase()));
        }

        if (priceRange != null && !priceRange.isBlank()) {
            spec = spec.and((root, query, cb) -> {
                switch (priceRange) {
                    case "low":
                        return cb.lessThan(root.get("price"), 600000);
                    case "medium":
                        return cb.between(root.get("price"), 600000, 1000000);
                    case "high":
                        return cb.greaterThan(root.get("price"), 1000000);
                    default:
                        return null;
                }
            });
        }

        if (year != null && !year.isBlank()) {
            try {
                int yr = Integer.parseInt(year);
                spec = spec.and((root, query, cb) ->
                        cb.equal(root.get("year"), yr));
            } catch (NumberFormatException ignored) {}
        }

        Page<Motorcycle> result = repo.findAll(spec, pageable);
        return result.map(this::toDto);
    }

    // Read one
    public MotorcycleResponseDTO getOne(Long id) {
        Motorcycle m = repo.findById(id).orElseThrow(() -> new RuntimeException("Motorcycle not found"));
        return toDto(m);
    }

    // Update
    public MotorcycleResponseDTO update(Long id, MotorcycleRequestDTO dto) throws IOException {
        Motorcycle existing = repo.findById(id).orElseThrow(() -> new RuntimeException("Motorcycle not found"));

        // copy allowed props
        existing.setBrand(dto.getBrand());
        existing.setModel(dto.getModel());
        existing.setType(dto.getType());
        existing.setEngineCapacity(dto.getEngineCapacity());
        existing.setStatus(dto.getStatus());
        existing.setPrice(dto.getPrice());
        existing.setLocation(dto.getLocation());
        existing.setOwner(dto.getOwner());
        existing.setDescription(dto.getDescription());
        existing.setFeatures(dto.getFeatures());

        // upload new images if provided (replace existing)
        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            existing.setImageUrls(uploadImages(dto.getImages()));
        }

        Motorcycle saved = repo.save(existing);
        return toDto(saved);
    }

    // Delete
    public void delete(Long id) {
        repo.deleteById(id);
    }

    private MotorcycleResponseDTO toDto(Motorcycle m) {
        return MotorcycleResponseDTO.builder()
                .id(m.getId())
                .brand(m.getBrand())
                .model(m.getModel())
                .type(m.getType())
                .engineCapacity(m.getEngineCapacity())
                .status(m.getStatus())
                .price(m.getPrice())
                .location(m.getLocation())
                .owner(m.getOwner())
                .description(m.getDescription())
                .features(m.getFeatures())
                .imageUrls(m.getImageUrls())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }

}

