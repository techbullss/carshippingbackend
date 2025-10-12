package io.reflectoring.carshippingbackend.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import io.reflectoring.carshippingbackend.DTO.ContainerDTO;
import io.reflectoring.carshippingbackend.DTO.ContainerResponseDTO;

import io.reflectoring.carshippingbackend.repository.ContainerRepository;
import io.reflectoring.carshippingbackend.tables.Container;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContainerService {

    private final ContainerRepository repository;
    private final Cloudinary cloudinary;

    public ContainerResponseDTO saveContainer(ContainerDTO dto) throws IOException {
        List<String> urls = uploadToCloudinary(dto.getImages());

        Container container = Container.builder()
                .containerNumber(dto.getContainerNumber())
                .size(dto.getSize())
                .type(dto.getType())
                .price(dto.getPrice())
                .status(dto.getStatus())
                .imageUrls(urls)
                .build();

        return toDto(repository.save(container));
    }

    public ContainerResponseDTO updateContainer(Long id, ContainerDTO dto) throws IOException {
        Container container = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Container not found"));

        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            List<String> urls = uploadToCloudinary(dto.getImages());
            container.setImageUrls(urls);
        }

        container.setContainerNumber(dto.getContainerNumber());
        container.setSize(dto.getSize());
        container.setType(dto.getType());
        container.setPrice(dto.getPrice());
        container.setStatus(dto.getStatus());

        return toDto(repository.save(container));
    }

    public void deleteContainer(Long id) {
        repository.deleteById(id);
    }

    public ContainerResponseDTO getContainer(Long id) {
        return repository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Container not found"));
    }

    public Page<ContainerResponseDTO> searchContainers(int page, int size, String search, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        Page<Container> result;
        if (search != null && !search.isEmpty()) {
            result = repository.findByContainerNumberContainingIgnoreCaseOrTypeContainingIgnoreCase(search, search, pageable);
        } else if (status != null && !status.isEmpty()) {
            result = repository.findByStatusIgnoreCase(status, pageable);
        } else {
            result = repository.findAll(pageable);
        }

        return result.map(this::toDto);
    }

    // 🔹 Upload images to Cloudinary
    private List<String> uploadToCloudinary(List<MultipartFile> images) throws IOException {
        if (images == null || images.isEmpty()) return List.of();

        List<String> urls = new ArrayList<>();
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
        return urls;
    }

    private ContainerResponseDTO toDto(Container c) {
        return ContainerResponseDTO.builder()
                .id(c.getId())
                .containerNumber(c.getContainerNumber())
                .size(c.getSize())
                .type(c.getType())
                .price(c.getPrice())
                .status(c.getStatus())
                .imageUrls(c.getImageUrls())
                .build();
    }
}
