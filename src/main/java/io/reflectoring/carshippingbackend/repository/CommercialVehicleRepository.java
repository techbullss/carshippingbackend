package io.reflectoring.carshippingbackend.repository;

import io.reflectoring.carshippingbackend.tables.Car;
import io.reflectoring.carshippingbackend.tables.CommercialVehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;

public interface CommercialVehicleRepository extends JpaRepository<CommercialVehicle, Long> {

    // ------------------- Search by brand or model -------------------
    Page<CommercialVehicle>
    findByBrandContainingIgnoreCaseOrModelContainingIgnoreCaseAndStatusIgnoreCase(
            String brand,
            String model,
            String status,
            Pageable pageable
    );

    Page<CommercialVehicle>
    findByTypeIgnoreCaseAndStatusIgnoreCase(
            String type,
            String status,
            Pageable pageable
    );
    Page<CommercialVehicle> findByStatusIgnoreCase(String status, Pageable pageable);

    // Add ownerEmail field to your CommercialVehicle entity if needed
    Page<CommercialVehicle> findByOwnerTypeIgnoreCase(String seller, Pageable pageable);
    Page<CommercialVehicle>
    findByBrandContainingIgnoreCaseOrModelContainingIgnoreCaseAndTypeIgnoreCaseAndStatusIgnoreCase(
            String brand,
            String model,
            String type,
            String status,
            Pageable pageable
    );

    // ------------------- Distinct makes with count -------------------
    @Query("SELECT NEW map(c.brand AS make, COUNT(c) AS count) " +
            "FROM CommercialVehicle c GROUP BY c.brand ORDER BY COUNT(c) DESC")
    List<Map<String, Object>> findDistinctMakesWithCount();

    // ------------------- Distinct models by make -------------------
    @Query("SELECT NEW map(c.model AS model, COUNT(c) AS count) " +
            "FROM CommercialVehicle c WHERE c.brand = :brand GROUP BY c.model ORDER BY COUNT(c) DESC")
    List<Map<String, Object>> findDistinctModelsByMake(String brand);

    // ------------------- Similar vehicles excluding a specific id -------------------
    @Query("SELECT c FROM CommercialVehicle c " +
            "WHERE (LOWER(c.brand) LIKE LOWER(CONCAT('%', :brand, '%')) " +
            "OR LOWER(c.model) LIKE LOWER(CONCAT('%', :model, '%'))) " +
            "AND (:excludeId IS NULL OR c.id <> :excludeId)")
    List<CommercialVehicle> findByBrandContainingIgnoreCaseOrModelContainingIgnoreCaseAndIdNot(
            String brand, String model, Long excludeId
    );
    @Query("""
SELECT c FROM Car c
WHERE 
    (:#{#filters == null || #filters['brand'] == null || #filters['brand'] == ''} = true OR c.brand LIKE %:#{#filters['brand']}%)
AND 
    (:#{#filters == null || #filters['model'] == null || #filters['model'] == ''} = true OR c.model LIKE %:#{#filters['model']}%)
AND 
    LOWER(c.seller) = LOWER(:email)
""")
    Page<CommercialVehicle> searchBySeller(@Param("filters") Map<String, String> filters, Pageable pageable, @Param("email") String email);
    @Query("""
SELECT c FROM CommercialVehicle c
WHERE 
    (:#{#filters == null || #filters['brand'] == null || #filters['brand'] == ''} = true OR c.brand LIKE %:#{#filters['brand']}%)
AND 
    (:#{#filters == null || #filters['model'] == null || #filters['model'] == ''} = true OR c.model LIKE %:#{#filters['model']}%)
""")
    Page<CommercialVehicle> search(@Param("filters") Map<String, String> filters, Pageable pageable);
}
