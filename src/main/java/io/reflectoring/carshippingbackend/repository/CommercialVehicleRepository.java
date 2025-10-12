package io.reflectoring.carshippingbackend.repository;

import io.reflectoring.carshippingbackend.tables.CommercialVehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommercialVehicleRepository extends JpaRepository<CommercialVehicle, Long> {

    Page<CommercialVehicle> findByBrandContainingIgnoreCaseOrModelContainingIgnoreCase(
            String brand, String model, Pageable pageable
    );

    Page<CommercialVehicle> findByTypeIgnoreCase(String type, Pageable pageable);

    Page<CommercialVehicle> findByBrandContainingIgnoreCaseOrModelContainingIgnoreCaseAndTypeIgnoreCase(
            String brand, String model, String type, Pageable pageable
    );
}
