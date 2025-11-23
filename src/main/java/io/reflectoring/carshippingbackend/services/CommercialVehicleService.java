package io.reflectoring.carshippingbackend.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import io.reflectoring.carshippingbackend.DTO.CommercialVehicleDTO;
import io.reflectoring.carshippingbackend.DTO.CommercialVehicleResponseDTO;
import io.reflectoring.carshippingbackend.Enum.Role;
import io.reflectoring.carshippingbackend.repository.CommercialVehicleRepository;
import io.reflectoring.carshippingbackend.tables.CommercialVehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
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
    private CommercialVehicleResponseDTO toDto(CommercialVehicle vehicle) {
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
        // Implement your approval logic (e.g., set status = "APPROVED")
        // vehicle.setStatus("APPROVED");
        repo.save(vehicle);
        return toDto(vehicle);
    }

    public CommercialVehicleResponseDTO rejectVehicle(Long id, String reason) {
        CommercialVehicle vehicle = repo.findById(id).orElseThrow(() -> new RuntimeException("Vehicle not found"));
        // Implement your reject logic (e.g., set status = "REJECTED" and reason)
        // vehicle.setStatus("REJECTED");
        // vehicle.setRejectReason(reason);
        repo.save(vehicle);
        return toDto(vehicle);
    }

    // ------------------- Dashboard (User Role) -------------------
    public Page<CommercialVehicleResponseDTO> dashboardByUser(String email, String role, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "priceKes"));
        Page<CommercialVehicle> results;
        // Example: admins see all, others see their vehicles
        if (role.equals(Role.ADMIN.name())) {
            results = repo.findAll(pageable);
        } else {
            // Add filtering by owner email if your entity has it
            // results = repo.findByOwnerEmail(email, pageable);
            results = repo.findAll(pageable); // Placeholder
        }
        return results.map(this::toDto);
    }

    // ------------------- Latest Arrivals -------------------
    public List<CommercialVehicleResponseDTO> getLatestArrivals() {
        Pageable pageable = PageRequest.of(0, 6, Sort.by(Sort.Direction.DESC, "id"));
        return repo.findAll(pageable).getContent().stream().map(this::toDto).toList();
    }

    // ------------------- Similar Vehicles -------------------
    public List<CommercialVehicleResponseDTO> getSimilarVehicles(String brand, String model, Long excludeId) {
        List<CommercialVehicle> vehicles = repo.findByBrandContainingIgnoreCaseOrModelContainingIgnoreCase(brand, model, PageRequest.of(0, 10)).getContent();
        if (excludeId != null) vehicles.removeIf(v -> v.getId().equals(excludeId));
        return vehicles.stream().map(this::toDto).toList();
    }

    // ------------------- Search -------------------
    public Page<CommercialVehicleResponseDTO> searchVehicles(int page, int size, String search, String type, Sort sort) {
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<CommercialVehicle> results;
        if (search != null && !search.isBlank() && type != null && !type.isBlank()) {
            results = repo.findByBrandContainingIgnoreCaseOrModelContainingIgnoreCaseAndTypeIgnoreCase(search, search, type, pageable);
        } else if (search != null && !search.isBlank()) {
            results = repo.findByBrandContainingIgnoreCaseOrModelContainingIgnoreCase(search, search, pageable);
        } else if (type != null && !type.isBlank()) {
            results = repo.findByTypeIgnoreCase(type, pageable);
        } else {
            results = repo.findAll(pageable);
        }
        return results.map(this::toDto);
    }
}
