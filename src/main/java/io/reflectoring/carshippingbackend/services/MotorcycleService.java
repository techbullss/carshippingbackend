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

    // ==================== HELPER METHODS ====================

    // Upload single image
    private String uploadSingleImage(MultipartFile image) throws IOException {
        String uniqueFileName = UUID.randomUUID() + "-" +
                Objects.requireNonNull(image.getOriginalFilename()).replaceAll("\\s+", "_");
        Map uploadResult = cloudinary.uploader().upload(
                image.getBytes(),
                ObjectUtils.asMap(
                        "public_id", "uploads/motorcycle/" + uniqueFileName,
                        "resource_type", "auto"
                )
        );
        return (String) uploadResult.get("secure_url");
    }

    // Upload multiple images
    private List<String> uploadImages(MultipartFile[] images) throws IOException {
        List<String> urls = new ArrayList<>();
        for (MultipartFile image : images) {
            if (!image.isEmpty()) {
                urls.add(uploadSingleImage(image));
            }
        }
        return urls;
    }

    // Convert Entity → DTO
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

    // ==================== CRUD OPERATIONS ====================

    // CREATE WITH DTO
    public MotorcycleResponseDTO createMotorcycle(MotorcycleRequestDTO dto) throws IOException {

        System.out.println("=== SERVICE: CREATE FROM DTO ===");
        System.out.println("DTO: " + dto);

        Motorcycle motorcycle = new Motorcycle();

        // REQUIRED FIELDS
        motorcycle.setBrand(dto.getBrand());
        motorcycle.setModel(dto.getModel());

        // OPTIONAL FIELDS
        motorcycle.setType(dto.getType());
        motorcycle.setLocation(dto.getLocation());
        motorcycle.setDescription(dto.getDescription());

        // STATUS (default to PENDING if null)
        motorcycle.setStatus(dto.getStatus() != null ? dto.getStatus() : "PENDING");

        // NUMERIC FIELDS (DTO already has correct types)
        motorcycle.setEngineCapacity(dto.getEngineCapacity());
        motorcycle.setPrice(dto.getPrice());
        motorcycle.setYear(dto.getYear());

        // FEATURES (already List<String> in DTO)
        motorcycle.setFeatures(dto.getFeatures());

        // UPLOAD IMAGES from DTO
        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            List<String> imageUrls = new ArrayList<>();
            for (MultipartFile image : dto.getImages()) {
                if (image != null && !image.isEmpty()) {
                    try {
                        String url = uploadSingleImage(image);
                        imageUrls.add(url);
                        System.out.println("Uploaded image: " + url);
                    } catch (IOException e) {
                        System.err.println("Failed to upload image: " + e.getMessage());
                    }
                }
            }
            motorcycle.setImageUrls(imageUrls);
        }

        // DEBUG LOG
        System.out.println("Saving motorcycle:");
        System.out.println("  Brand: " + motorcycle.getBrand());
        System.out.println("  Model: " + motorcycle.getModel());
        System.out.println("  Price: " + motorcycle.getPrice());
        System.out.println("  Year: " + motorcycle.getYear());
        System.out.println("  Status: " + motorcycle.getStatus());
        System.out.println("  Features: " + motorcycle.getFeatures());
        System.out.println("  Image count: " + (motorcycle.getImageUrls() != null ? motorcycle.getImageUrls().size() : 0));

        try {
            Motorcycle saved = repo.save(motorcycle);
            System.out.println("SAVED successfully with ID: " + saved.getId());
            return toDto(saved);
        } catch (Exception e) {
            System.err.println("SAVE ERROR: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to save motorcycle: " + e.getMessage());
        }
    }

    // UPDATE
    public MotorcycleResponseDTO updateMotorcycle(
            Long id,
            Map<String, String> motorcycleData,
            MultipartFile[] images) throws IOException {

        Motorcycle existing = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Motorcycle not found with id " + id));

        // Update only provided fields (partial update)
        if (motorcycleData.containsKey("brand")) existing.setBrand(motorcycleData.get("brand"));
        if (motorcycleData.containsKey("model")) existing.setModel(motorcycleData.get("model"));
        if (motorcycleData.containsKey("type")) existing.setType(motorcycleData.get("type"));

        // Parse and update numeric fields
        if (motorcycleData.containsKey("engineCapacity")) {
            try {
                existing.setEngineCapacity(Integer.parseInt(motorcycleData.get("engineCapacity")));
            } catch (NumberFormatException e) {
                // Ignore invalid value
            }
        }

        if (motorcycleData.containsKey("price")) {
            try {
                existing.setPrice(Double.parseDouble(motorcycleData.get("price")));
            } catch (NumberFormatException e) {
                // Ignore invalid value
            }
        }

        if (motorcycleData.containsKey("year")) {
            try {
                existing.setYear(Integer.parseInt(motorcycleData.get("year")));
            } catch (NumberFormatException e) {
                // Ignore invalid value
            }
        }

        if (motorcycleData.containsKey("location")) existing.setLocation(motorcycleData.get("location"));
        if (motorcycleData.containsKey("status")) existing.setStatus(motorcycleData.get("status"));
        if (motorcycleData.containsKey("description")) existing.setDescription(motorcycleData.get("description"));

        // Handle features update
        if (motorcycleData.containsKey("features") &&
                motorcycleData.get("features") != null) {
            String featuresStr = motorcycleData.get("features");
            if (featuresStr.startsWith("[") && featuresStr.endsWith("]")) {
                featuresStr = featuresStr.substring(1, featuresStr.length() - 1)
                        .replace("\"", "");
            }
            if (!featuresStr.isBlank()) {
                List<String> features = Arrays.stream(featuresStr.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
                existing.setFeatures(features);
            } else {
                existing.setFeatures(null);
            }
        }

        // Update images if provided
        if (images != null && images.length > 0) {
            existing.setImageUrls(uploadImages(images));
        }

        Motorcycle updated = repo.save(existing);
        return toDto(updated);
    }

    // DELETE
    public void deleteMotorcycle(Long id) {
        repo.deleteById(id);
    }

    // ==================== ADMIN OPERATIONS ====================

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
        // Optional: Add rejection reason to entity if needed
        repo.save(motorcycle);
        return toDto(motorcycle);
    }

    // ==================== NEW METHODS FOR DASHBOARD & PUBLIC ENDPOINTS ====================

    // 1. Search by filters (used by public endpoint)
    public Page<MotorcycleResponseDTO> searchByFilters(
            Map<String, String> filters,
            int page, int size, Sort sort) {

        Pageable pageable = PageRequest.of(page, size, sort);

        // Always filter to APPROVED for public access unless specified
        if (!filters.containsKey("status")) {
            filters.put("status", "APPROVED");
        }

        Specification<Motorcycle> spec = MotorcycleSpecification.byFilters(filters);
        Page<Motorcycle> results = repo.findAll(spec, pageable);

        return results.map(this::toDto);
    }

    // 2. Get single motorcycle by ID
    public MotorcycleResponseDTO getOne(Long id) {
        Motorcycle motorcycle = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Motorcycle not found with id: " + id));
        return toDto(motorcycle);
    }

    // 3. Role-based search (for dashboard)
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

    // ==================== EXISTING SEARCH & FILTER METHODS ====================

    // Public search (APPROVED only)
    public Page<MotorcycleResponseDTO> searchApproved(
            Map<String, String> allParams,
            int page, int size, Sort sort) {

        Pageable pageable = PageRequest.of(page, size, sort);
        Map<String, String> filters = new HashMap<>(allParams);

        // Force APPROVED status for public access
        filters.put("status", "APPROVED");

        Specification<Motorcycle> spec = MotorcycleSpecification.byFilters(filters);
        Page<Motorcycle> results = repo.findAll(spec, pageable);

        return results.map(this::toDto);
    }

    // Specification-based search methods (internal)
    private Page<Motorcycle> searchWithSpecifications(
            Map<String, String> filters, Pageable pageable) {
        Specification<Motorcycle> spec = MotorcycleSpecification.byFilters(filters);
        return repo.findAll(spec, pageable);
    }

    private Page<Motorcycle> searchBySellerWithSpecifications(
            Map<String, String> filters, String sellerEmail, Pageable pageable) {
        Specification<Motorcycle> spec = MotorcycleSpecification
                .byOwner(sellerEmail)
                .and(MotorcycleSpecification.byFilters(filters));
        return repo.findAll(spec, pageable);
    }

    private Page<Motorcycle> searchPublicWithSpecifications(
            Map<String, String> filters, Pageable pageable) {
        Specification<Motorcycle> spec = MotorcycleSpecification
                .byApprovedStatus()
                .and(MotorcycleSpecification.byFilters(filters));
        return repo.findAll(spec, pageable);
    }

    // Legacy filter endpoint
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
                filters.put("search", year);
            }
        }

        filters.put("status", "APPROVED");

        Pageable pageable = PageRequest.of(page, size);
        Specification<Motorcycle> spec = MotorcycleSpecification.byFilters(filters);
        Page<Motorcycle> results = repo.findAll(spec, pageable);

        return results.map(this::toDto);
    }

    // Legacy search (for backward compatibility)
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
            results = repo.findByStatus("APPROVED", pageable);
        }

        return results.map(this::toDto);
    }

    // ==================== ADDITIONAL FEATURES ====================

    // Latest arrivals
    public List<MotorcycleResponseDTO> getLatestArrivals() {
        Pageable pageable = PageRequest.of(0, 6, Sort.by(Sort.Direction.DESC, "createdAt"));
        return repo.findAll(pageable).getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // Similar motorcycles
    public List<MotorcycleResponseDTO> getSimilarMotorcycles(String brand, String model, Long excludeId) {
        List<Motorcycle> motorcycles = repo.findByBrandAndModelAndIdNot(brand, model, excludeId);
        return motorcycles.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // Get distinct brands with count (for filter options)
    public List<Map<String, Object>> getDistinctBrandsWithCount() {
        return repo.findDistinctBrandsWithCount();
    }

    // Get distinct models by brand
    public List<Map<String, Object>> getDistinctModelsByBrand(String brand) {
        return repo.findDistinctModelsByBrand(brand);
    }

    // Filter options for dropdowns
    public Map<String, List<String>> getFilterOptions() {
        Map<String, List<String>> options = new HashMap<>();

        List<String> brands = repo.findAll().stream()
                .map(Motorcycle::getBrand)
                .filter(brand -> brand != null && !brand.trim().isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        options.put("brands", brands);

        List<String> types = repo.findAll().stream()
                .map(Motorcycle::getType)
                .filter(type -> type != null && !type.trim().isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        options.put("types", types);

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