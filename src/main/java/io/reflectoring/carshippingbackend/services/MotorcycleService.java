package io.reflectoring.carshippingbackend.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import io.reflectoring.carshippingbackend.DTO.MotorcycleRequestDTO;
import io.reflectoring.carshippingbackend.DTO.MotorcycleResponseDTO;
import io.reflectoring.carshippingbackend.repository.MotorcycleRepository;
import io.reflectoring.carshippingbackend.tables.Motorcycle;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MotorcycleService {

    private final MotorcycleRepository repo;
    private final Cloudinary cloudinary;

    // ------------------- Upload Images -------------------
    private List<String> uploadImages(List<MultipartFile> images) throws IOException {
        List<String> urls = new ArrayList<>();
        for (MultipartFile f : images) {
            String uniqueFileName = UUID.randomUUID() + "-" + Objects.requireNonNull(f.getOriginalFilename()).replaceAll("\\s+", "_");
            Map uploadResult = cloudinary.uploader().upload(
                    f.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", "uploads/motorcycle/" + uniqueFileName,
                            "resource_type", "auto"
                    )
            );
            urls.add((String) uploadResult.get("secure_url"));
        }
        return urls;
    }

    // ------------------- Convert Entity → DTO -------------------
    public MotorcycleResponseDTO toDto(Motorcycle motorcycle) {
        return MotorcycleResponseDTO.builder()
                .id(motorcycle.getId())
                .brand(motorcycle.getBrand())
                .model(motorcycle.getModel())
                .type(motorcycle.getType())
                .engineCapacity(motorcycle.getEngineCapacity())
                .status(motorcycle.getStatus())
                .price(motorcycle.getPrice())
                .location(motorcycle.getLocation())
                .owner(motorcycle.getOwner())
                .year(motorcycle.getYear())
                .features(motorcycle.getFeatures())
                .imageUrls(motorcycle.getImageUrls())
                .description(motorcycle.getDescription())
                .createdAt(motorcycle.getCreatedAt())
                .updatedAt(motorcycle.getUpdatedAt())
                .build();
    }

    // ------------------- Map DTO → Entity -------------------
    private void mapDtoToEntity(MotorcycleRequestDTO dto, Motorcycle motorcycle) {
        motorcycle.setBrand(dto.getBrand());
        motorcycle.setModel(dto.getModel());
        motorcycle.setType(dto.getType());
        motorcycle.setEngineCapacity(dto.getEngineCapacity());
        motorcycle.setStatus(dto.getStatus() != null ? dto.getStatus() : "PENDING");
        motorcycle.setPrice(dto.getPrice());
        motorcycle.setLocation(dto.getLocation());
        motorcycle.setOwner(dto.getOwner());
        motorcycle.setYear(dto.getYear());
        motorcycle.setFeatures(dto.getFeatures());
        motorcycle.setDescription(dto.getDescription());
    }

    // ------------------- CREATE -------------------
    public MotorcycleResponseDTO createMotorcycle(MotorcycleRequestDTO dto, String userEmail) throws IOException {
        Motorcycle motorcycle = new Motorcycle();
        mapDtoToEntity(dto, motorcycle);

        // Set owner from authenticated user
        motorcycle.setOwner(userEmail);

        // Upload images if provided
        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            motorcycle.setImageUrls(uploadImages(dto.getImages()));
        }

        Motorcycle saved = repo.save(motorcycle);
        return toDto(saved);
    }

    // ------------------- READ -------------------
    public MotorcycleResponseDTO getMotorcycle(Long id) {
        Motorcycle motorcycle = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Motorcycle not found with id " + id));
        return toDto(motorcycle);
    }

    // ------------------- UPDATE -------------------
    public MotorcycleResponseDTO updateMotorcycle(Long id, MotorcycleRequestDTO dto) throws IOException {
        Motorcycle existing = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Motorcycle not found with id " + id));

        mapDtoToEntity(dto, existing);

        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            existing.setImageUrls(uploadImages(dto.getImages()));
        }

        Motorcycle updated = repo.save(existing);
        return toDto(updated);
    }

    // ------------------- DELETE -------------------
    public void deleteMotorcycle(Long id) {
        repo.deleteById(id);
    }

    // ------------------- APPROVE / REJECT -------------------
    public MotorcycleResponseDTO approveMotorcycle(Long id) {
        Motorcycle motorcycle = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Motorcycle not found"));
        motorcycle.setStatus("APPROVED");
        repo.save(motorcycle);
        return toDto(motorcycle);
    }

    public MotorcycleResponseDTO rejectMotorcycle(Long id, String reason) {
        Motorcycle motorcycle = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Motorcycle not found"));
        motorcycle.setStatus("REJECTED");
        // You could add a rejection reason field to your entity if needed
        repo.save(motorcycle);
        return toDto(motorcycle);
    }

    // ============= SPECIFICATION-BASED SEARCH METHODS =============

    public Page<Motorcycle> searchWithSpecifications(
            Map<String, String> filters,
            Pageable pageable) {

        Specification<Motorcycle> spec = MotorcycleSpecification.byFilters(filters);
        return repo.findAll(spec, pageable);
    }

    public Page<Motorcycle> searchBySellerWithSpecifications(
            Map<String, String> filters,
            String sellerEmail,
            Pageable pageable) {

        Specification<Motorcycle> spec = MotorcycleSpecification
                .byOwner(sellerEmail)
                .and(MotorcycleSpecification.byFilters(filters));

        return repo.findAll(spec, pageable);
    }

    public Page<Motorcycle> searchPublicWithSpecifications(
            Map<String, String> filters,
            Pageable pageable) {

        Specification<Motorcycle> spec = MotorcycleSpecification
                .byApprovedStatus()
                .and(MotorcycleSpecification.byFilters(filters));

        return repo.findAll(spec, pageable);
    }

    // ------------------- ROLE-BASED SEARCH -------------------
    public Page<MotorcycleResponseDTO> searchByUserRole(
            Map<String, String> allParams,
            int page, int size, Sort sort,
            String currentUserEmail, String currentUserRole) {

        Pageable pageable = PageRequest.of(page, size, sort);
        String role = currentUserRole.replace("ROLE_", "").toUpperCase();

        Page<Motorcycle> results;

        switch (role) {
            case "ADMIN":
                results = searchWithSpecifications(allParams, pageable);
                break;
            case "SELLER":
                results = searchBySellerWithSpecifications(allParams, currentUserEmail, pageable);
                break;
            default: // PUBLIC or unauthenticated
                results = searchPublicWithSpecifications(allParams, pageable);
                break;
        }

        return results.map(this::toDto);
    }

    // ------------------- LEGACY SEARCH (for backward compatibility) -------------------
    public Page<MotorcycleResponseDTO> search(
            int page, int size,
            String search, String type, String status) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Motorcycle> results;

        if (search != null && !search.isBlank() && type != null && !type.isBlank()) {
            results = repo.findByBrandContainingIgnoreCaseOrModelContainingIgnoreCaseAndStatus(
                    search, search, status != null ? status : "APPROVED", pageable);
        } else if (search != null && !search.isBlank()) {
            results = repo.findByBrandContainingIgnoreCaseOrModelContainingIgnoreCaseAndStatus(
                    search, search, status != null ? status : "APPROVED", pageable);
        } else if (type != null && !type.isBlank()) {
            results = repo.findByTypeAndStatus(type, status != null ? status : "APPROVED", pageable);
        } else if (status != null && !status.isBlank()) {
            results = repo.findByStatus(status, pageable);
        } else {
            // Default: show only APPROVED for public
            results = repo.findByStatus("APPROVED", pageable);
        }

        return results.map(this::toDto);
    }

    // ------------------- FILTER MOTORCYCLES (legacy endpoint) -------------------
    public Page<MotorcycleResponseDTO> filterMotorcycles(
            int page, int size,
            String make, String type,
            String priceRange, String year) {

        Map<String, String> filters = new HashMap<>();
        if (make != null && !make.isBlank()) filters.put("brand", make);
        if (type != null && !type.isBlank()) filters.put("type", type);
        if (priceRange != null && !priceRange.isBlank()) filters.put("priceRange", priceRange);
        if (year != null && !year.isBlank()) {
            try {
                int yearInt = Integer.parseInt(year);
                filters.put("minYear", String.valueOf(yearInt));
                filters.put("maxYear", String.valueOf(yearInt));
            } catch (NumberFormatException e) {
                // If not a number, treat as search
                filters.put("search", year);
            }
        }

        // Always filter by APPROVED for public access
        filters.put("status", "APPROVED");

        Pageable pageable = PageRequest.of(page, size);
        Specification<Motorcycle> spec = MotorcycleSpecification.byFilters(filters);
        Page<Motorcycle> results = repo.findAll(spec, pageable);

        return results.map(this::toDto);
    }

    // ------------------- LATEST ARRIVALS -------------------
    public List<MotorcycleResponseDTO> getLatestArrivals() {
        Pageable pageable = PageRequest.of(0, 6, Sort.by(Sort.Direction.DESC, "createdAt"));
        return repo.findAll(pageable).getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ------------------- SIMILAR MOTORCYCLES -------------------
    public List<MotorcycleResponseDTO> getSimilarMotorcycles(String brand, String model, Long excludeId) {
        List<Motorcycle> motorcycles = repo.findByBrandAndModelAndIdNot(brand, model, excludeId);
        return motorcycles.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ------------------- GET FILTER OPTIONS -------------------
    public Map<String, List<String>> getFilterOptions() {
        Map<String, List<String>> options = new HashMap<>();

        // Get distinct brands
        List<String> brands = repo.findAll().stream()
                .map(Motorcycle::getBrand)
                .filter(brand -> brand != null && !brand.trim().isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        options.put("brands", brands);

        // Get distinct types
        List<String> types = repo.findAll().stream()
                .map(Motorcycle::getType)
                .filter(type -> type != null && !type.trim().isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        options.put("types", types);

        // Get distinct locations
        List<String> locations = repo.findAll().stream()
                .map(Motorcycle::getLocation)
                .filter(location -> location != null && !location.trim().isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        options.put("locations", locations);

        return options;
    }
}
