package io.reflectoring.carshippingbackend.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import io.reflectoring.carshippingbackend.DTO.CommercialVehicleDTO;
import io.reflectoring.carshippingbackend.DTO.CommercialVehicleResponseDTO;
import io.reflectoring.carshippingbackend.repository.CommercialVehicleRepository;
import io.reflectoring.carshippingbackend.tables.CommercialVehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CommercialVehicleService {

    private final CommercialVehicleRepository repo;
    private final Cloudinary cloudinary;

    // ------------------- Upload Images -------------------
    private List<String> uploadImages(List<MultipartFile> images) throws IOException {
        List<String> urls = new ArrayList<>();
        for (MultipartFile f : images) {
            String uniqueFileName = UUID.randomUUID() + "-" + Objects.requireNonNull(f.getOriginalFilename()).replaceAll("\\s+", "_");
            Map uploadResult = cloudinary.uploader().upload(
                    f.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", "uploads/" + uniqueFileName,
                            "resource_type", "auto"
                    )
            );
            urls.add((String) uploadResult.get("secure_url"));
        }
        return urls;
    }

    // ------------------- Convert Entity → DTO -------------------
    public CommercialVehicleResponseDTO toDto(CommercialVehicle vehicle) {
        CommercialVehicleResponseDTO dto = new CommercialVehicleResponseDTO();
        dto.setId(vehicle.getId());
        dto.setBrand(vehicle.getBrand());
        dto.setModel(vehicle.getModel());
        dto.setType(vehicle.getType());
        dto.setYearOfManufacture(vehicle.getYearOfManufacture());
        dto.setConditionType(vehicle.getConditionType());
        dto.setBodyType(vehicle.getBodyType());
        dto.setColor(vehicle.getColor());
        dto.setEngineType(vehicle.getEngineType());
        dto.setEngineCapacityCc(vehicle.getEngineCapacityCc());
        dto.setFuelType(vehicle.getFuelType());
        dto.setTransmission(vehicle.getTransmission());
        dto.setSeats(vehicle.getSeats());
        dto.setDoors(vehicle.getDoors());
        dto.setMileageKm(vehicle.getMileageKm());
        dto.setPayloadCapacityKg(vehicle.getPayloadCapacityKg());
        dto.setCargoVolumeM3(vehicle.getCargoVolumeM3());
        dto.setSleeperCapacity(vehicle.getSleeperCapacity());
        dto.setCamperFeatures(vehicle.getCamperFeatures());
        dto.setPriceKes(vehicle.getPriceKes());
        dto.setDescription(vehicle.getDescription());
        dto.setLocation(vehicle.getLocation());
        dto.setOwnerType(vehicle.getOwnerType());
        dto.setFeatures(vehicle.getFeatures());
        dto.setCustomSpecs(vehicle.getCustomSpecs());
        dto.setImageUrls(vehicle.getImageUrls());
        return dto;
    }

    // ------------------- Map DTO → Entity -------------------
    private void mapDtoToEntity(CommercialVehicleDTO dto, CommercialVehicle vehicle) {
        vehicle.setBrand(dto.getBrand());
        vehicle.setModel(dto.getModel());
        vehicle.setType(dto.getType());
        vehicle.setYearOfManufacture(dto.getYearOfManufacture());
        vehicle.setConditionType(dto.getConditionType());
        vehicle.setBodyType(dto.getBodyType());
        vehicle.setColor(dto.getColor());
        vehicle.setEngineType(dto.getEngineType());
        vehicle.setEngineCapacityCc(dto.getEngineCapacityCc());
        vehicle.setFuelType(dto.getFuelType());
        vehicle.setTransmission(dto.getTransmission());
        vehicle.setSeats(dto.getSeats());
        vehicle.setDoors(dto.getDoors());
        vehicle.setMileageKm(dto.getMileageKm());
        vehicle.setPayloadCapacityKg(dto.getPayloadCapacityKg());
        vehicle.setCargoVolumeM3(dto.getCargoVolumeM3());
        vehicle.setSleeperCapacity(dto.getSleeperCapacity());
        vehicle.setCamperFeatures(dto.getCamperFeatures());
        vehicle.setPriceKes(dto.getPriceKes());
        vehicle.setDescription(dto.getDescription());
        vehicle.setLocation(dto.getLocation());
        vehicle.setOwnerType(dto.getOwnerType());
        vehicle.setFeatures(dto.getFeatures());
        vehicle.setCustomSpecs(dto.getCustomSpecs());
        // ADD THIS LINE:
        vehicle.setSeller(dto.getSeller());
        vehicle.setStatus(dto.getStatus());
        vehicle.setOwnerEmail(dto.getSeller());
    }

    // ------------------- Create -------------------
    public CommercialVehicleResponseDTO createVehicle(CommercialVehicleDTO dto, String userEmail, String userRole) throws IOException {
        CommercialVehicle vehicle = new CommercialVehicle();
        mapDtoToEntity(dto, vehicle);
        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            vehicle.setImageUrls(uploadImages(dto.getImages()));
        }
        // You can add logic to set owner email, status, etc. if needed
        CommercialVehicle saved = repo.save(vehicle);
        return toDto(saved);
    }

    // ==================== SPECIFICATION-BASED SEARCH METHODS (FIXED) ====================

    public Page<CommercialVehicle> searchWithSpecifications(
            Map<String, String> filters,
            Pageable pageable) {

        Specification<CommercialVehicle> spec = CommercialVehicleSpecification.byFilters(filters);
        return repo.findAll(spec, pageable);
    }

    public Page<CommercialVehicle> searchBySellerWithSpecifications(
            Map<String, String> filters,
            String sellerEmail,
            Pageable pageable) {

        // FIXED: Removed deprecated Specification.where()
        Specification<CommercialVehicle> spec = CommercialVehicleSpecification
                .bySeller(sellerEmail)
                .and(CommercialVehicleSpecification.byFilters(filters));

        return repo.findAll(spec, pageable);
    }

    public Page<CommercialVehicle> searchPublicWithSpecifications(
            Map<String, String> filters,
            Pageable pageable) {

        // FIXED: Removed deprecated Specification.where()
        Specification<CommercialVehicle> spec = CommercialVehicleSpecification
                .byApprovedStatus()
                .and(CommercialVehicleSpecification.byFilters(filters));

        return repo.findAll(spec, pageable);
    }

    // ------------------- Read -------------------
    public CommercialVehicleResponseDTO getVehicle(Long id) {
        CommercialVehicle vehicle = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id " + id));
        return toDto(vehicle);
    }

    // ------------------- Update -------------------
    public CommercialVehicleResponseDTO updateVehicle(Long id, CommercialVehicleDTO dto) throws IOException {
        CommercialVehicle existing = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id " + id));
        mapDtoToEntity(dto, existing);
        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            existing.setImageUrls(uploadImages(dto.getImages()));
        }
        CommercialVehicle updated = repo.save(existing);
        return toDto(updated);
    }

    // ------------------- Delete -------------------
    public void deleteVehicle(Long id) {
        repo.deleteById(id);
    }

    // ------------------- Approve / Reject -------------------
    public CommercialVehicleResponseDTO approveVehicle(Long id) {
        CommercialVehicle vehicle = repo.findById(id).orElseThrow(() -> new RuntimeException("Vehicle not found"));
        vehicle.setStatus("APPROVED");  // Fixed: Use uppercase consistently
        repo.save(vehicle);
        return toDto(vehicle);
    }

    public CommercialVehicleResponseDTO rejectVehicle(Long id, String reason) {
        CommercialVehicle vehicle = repo.findById(id).orElseThrow(() -> new RuntimeException("Vehicle not found"));
        vehicle.setStatus("REJECTED");
        vehicle.setRejectionReason(reason);
        repo.save(vehicle);
        return toDto(vehicle);
    }

    // ------------------- Latest Arrivals -------------------
    public List<CommercialVehicleResponseDTO> getLatestArrivals() {
        Pageable pageable = PageRequest.of(0, 6, Sort.by(Sort.Direction.DESC, "id"));
        return repo.findAll(pageable).getContent().stream().map(this::toDto).toList();
    }

    // ------------------- Similar Vehicles -------------------
    public List<CommercialVehicleResponseDTO> getSimilarVehicles(String brand, String model, Long excludeId) {
        List<CommercialVehicle> vehicles =
                repo.findByBrandContainingIgnoreCaseOrModelContainingIgnoreCaseAndStatusIgnoreCase(
                        brand, model, "APPROVED", PageRequest.of(0, 10)  // Fixed: uppercase
                ).getContent();

        if (excludeId != null) {
            vehicles.removeIf(v -> v.getId().equals(excludeId));
        }
        return vehicles.stream().map(this::toDto).toList();
    }

    // ------------------- Old Search (Legacy method - keep for compatibility) -------------------
    // ------------------- Search with Specifications (NEW IMPROVED VERSION) -------------------
    public Page<CommercialVehicleResponseDTO> searchVehicles(
            int page, int size,
            String search, String type,
            Sort sort,
            Map<String, String> allFilters) {  // ADD THIS PARAMETER

        Pageable pageable = PageRequest.of(page, size, sort);

        // Build filters map from all parameters
        Map<String, String> filters = new HashMap<>();

        // Add basic search parameter (for backward compatibility)
        if (search != null && !search.isBlank()) {
            filters.put("search", search);
        }

        // Add type filter (for backward compatibility)
        if (type != null && !type.isBlank()) {
            filters.put("type", type);
        }

        // Add ALL other filters from the request
        if (allFilters != null) {
            filters.putAll(allFilters);
        }

        // Always filter by APPROVED status for public access
        if (!filters.containsKey("status")) {
            filters.put("status", "APPROVED");
        }

        // Use Specifications for filtering
        Specification<CommercialVehicle> spec = CommercialVehicleSpecification.byFilters(filters);
        Page<CommercialVehicle> results = repo.findAll(spec, pageable);

        return results.map(this::toDto);
    }

    // ------------------- Search by User Role -------------------
    public Page<CommercialVehicle> searchByUserRole(
            Map<String, String> allParams,
            int page, int size, Sort sort,
            String currentUserEmail, String currentUserRole) {

        Pageable pageable = PageRequest.of(page, size, sort);
        String role = currentUserRole.replace("ROLE_", "").toUpperCase();

        switch (role) {
            case "ADMIN":
                return searchWithSpecifications(allParams, pageable);
            case "SELLER":
                return searchBySellerWithSpecifications(allParams, currentUserEmail, pageable);
            default: // PUBLIC
                return searchPublicWithSpecifications(allParams, pageable);
        }
    }

    // ==================== NEW DTO-BASED SEARCH METHODS (Recommended) ====================

    public Page<CommercialVehicleResponseDTO> searchVehiclesDTO(
            Map<String, String> filters,
            int page, int size,
            String sortField, String sortDirection) {

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortField);
        Pageable pageable = PageRequest.of(page, size, sort);

        // Automatically add APPROVED status filter for public access
        if (!filters.containsKey("status")) {
            filters.put("status", "APPROVED");
        }

        Page<CommercialVehicle> vehicles = searchWithSpecifications(filters, pageable);
        return vehicles.map(this::toDto);
    }
}