package io.reflectoring.carshippingbackend.repository;

import io.reflectoring.carshippingbackend.tables.Motorcycle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;

public interface MotorcycleRepository extends
        JpaRepository<Motorcycle, Long>,
        JpaSpecificationExecutor<Motorcycle> {

    // ============= DERIVED QUERIES =============
    Page<Motorcycle> findByStatus(String status, Pageable pageable);
    Page<Motorcycle> findByBrandContainingIgnoreCaseOrModelContainingIgnoreCaseAndStatus(
            String brand, String model, String status, Pageable pageable);
    Page<Motorcycle> findByTypeAndStatus(String type, String status, Pageable pageable);
    Page<Motorcycle> findByOwner(String owner, Pageable pageable);

    // ============= CUSTOM QUERIES =============
    @Query("SELECT new map(m.brand as name, COUNT(m) as count) FROM Motorcycle m GROUP BY m.brand ORDER BY m.brand")
    List<Map<String, Object>> findDistinctBrandsWithCount();

    @Query("SELECT DISTINCT new map(m.model as name) FROM Motorcycle m WHERE m.brand = :brand ORDER BY m.model")
    List<Map<String, Object>> findDistinctModelsByBrand(@Param("brand") String brand);

    @Query("SELECT m FROM Motorcycle m WHERE m.brand = :brand AND m.model = :model AND m.id != :excludeId")
    List<Motorcycle> findByBrandAndModelAndIdNot(
            @Param("brand") String brand,
            @Param("model") String model,
            @Param("excludeId") Long excludeId);

    // ============= SEARCH BY SELLER (Legacy - matches your Car pattern) =============
    @Query("""
        SELECT m FROM Motorcycle m
        WHERE 
            (:#{#filters == null || #filters['brand'] == null || #filters['brand'] == ''} = true OR m.brand LIKE %:#{#filters['brand']}%)
        AND 
            (:#{#filters == null || #filters['model'] == null || #filters['model'] == ''} = true OR m.model LIKE %:#{#filters['model']}%)
        AND 
            LOWER(m.owner) = LOWER(:email)
        """)
    Page<Motorcycle> searchBySeller(@Param("filters") Map<String, String> filters, Pageable pageable, @Param("email") String email);
}