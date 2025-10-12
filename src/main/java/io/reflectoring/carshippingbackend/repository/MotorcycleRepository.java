package io.reflectoring.carshippingbackend.repository;
import io.reflectoring.carshippingbackend.tables.Motorcycle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MotorcycleRepository extends JpaRepository<Motorcycle, Long> {
    Page<Motorcycle> findByTypeIgnoreCase(String type, Pageable pageable);
    Page<Motorcycle> findByStatusIgnoreCase(String status, Pageable pageable);
    Page<Motorcycle> findByBrandContainingIgnoreCaseOrModelContainingIgnoreCase(String brand, String model, Pageable pageable);

    Page<Motorcycle> findAll(Specification<Motorcycle> spec, Pageable pageable);
}

