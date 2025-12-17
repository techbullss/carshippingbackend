package io.reflectoring.carshippingbackend.repository;

import io.reflectoring.carshippingbackend.tables.CommercialVehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;

public interface CommercialVehicleRepository extends
        JpaRepository<CommercialVehicle, Long>,
        JpaSpecificationExecutor<CommercialVehicle> {

    // ============= SIMILAR VEHICLES QUERY (matches CarRepository pattern) =============
    @Query("SELECT c FROM CommercialVehicle c WHERE c.brand = :brand AND c.model = :model AND c.id != :excludeId")
    List<CommercialVehicle> findByBrandAndModelAndIdNot(
            @Param("brand") String brand,
            @Param("model") String model,
            @Param("excludeId") Long excludeId);

    // ============= DISTINCT MAKES WITH COUNT (matches CarRepository pattern) =============
    @Query("SELECT new map(c.brand as name, COUNT(c) as count) FROM CommercialVehicle c GROUP BY c.brand ORDER BY c.brand")
    List<Map<String, Object>> findDistinctMakesWithCount();

    // ============= DISTINCT MODELS BY MAKE (matches CarRepository pattern) =============
    @Query("SELECT DISTINCT new map(c.model as name) FROM CommercialVehicle c WHERE c.brand = :make ORDER BY c.model")
    List<Map<String, Object>> findDistinctModelsByMake(@Param("make") String make);

    // ============= SEARCH WITH FILTERS (EXACT SAME PATTERN AS CAR REPOSITORY) =============
    @Query("""
        SELECT c FROM CommercialVehicle c
        WHERE 
            (:#{#filters == null || #filters['brand'] == null || #filters['brand'] == ''} = true OR c.brand LIKE %:#{#filters['brand']}%)
        AND 
            (:#{#filters == null || #filters['model'] == null || #filters['model'] == ''} = true OR c.model LIKE %:#{#filters['model']}%)
        """)
    Page<CommercialVehicle> search(@Param("filters") Map<String, String> filters, Pageable pageable);

    // ============= SEARCH BY SELLER (EXACT SAME PATTERN AS CAR REPOSITORY) =============
    @Query("""
        SELECT c FROM CommercialVehicle c
        WHERE 
            (:#{#filters == null || #filters['brand'] == null || #filters['brand'] == ''} = true OR c.brand LIKE %:#{#filters['brand']}%)
        AND 
            (:#{#filters == null || #filters['model'] == null || #filters['model'] == ''} = true OR c.model LIKE %:#{#filters['model']}%)
        AND 
            LOWER(c.seller) = LOWER(:email)
        """)
    Page<CommercialVehicle> searchBySeller(@Param("filters") Map<String, String> filters, Pageable pageable, @Param("email") String email);

    // ============= STATUS FILTER (matches CarRepository pattern) =============
    Page<CommercialVehicle> findByStatus(String status, Pageable pageable);

    // ============= OPTIONAL: Keep your existing derived queries for backward compatibility =============
    // These can coexist with the new ones above

    Page<CommercialVehicle> findByBrandContainingIgnoreCaseOrModelContainingIgnoreCaseAndStatusIgnoreCase(
            String brand,
            String model,
            String status,
            Pageable pageable
    );

    Page<CommercialVehicle> findByTypeIgnoreCaseAndStatusIgnoreCase(
            String type,
            String status,
            Pageable pageable
    );

    Page<CommercialVehicle> findByStatusIgnoreCase(String status, Pageable pageable);

    Page<CommercialVehicle> findByOwnerTypeIgnoreCase(String seller, Pageable pageable);

    Page<CommercialVehicle> findByBrandContainingIgnoreCaseOrModelContainingIgnoreCaseAndTypeIgnoreCaseAndStatusIgnoreCase(
            String brand,
            String model,
            String type,
            String status,
            Pageable pageable
    );

    // ============= OPTIONAL: Your original "similar vehicles" query =============
    @Query("SELECT c FROM CommercialVehicle c " +
            "WHERE (LOWER(c.brand) LIKE LOWER(CONCAT('%', :brand, '%')) " +
            "OR LOWER(c.model) LIKE LOWER(CONCAT('%', :model, '%'))) " +
            "AND (:excludeId IS NULL OR c.id <> :excludeId)")
    List<CommercialVehicle> findByBrandContainingIgnoreCaseOrModelContainingIgnoreCaseAndIdNot(
            String brand, String model, Long excludeId
    );
}