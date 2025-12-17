package io.reflectoring.carshippingbackend.repository;

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

    // ------------------- Seller search with all filters -------------------
    @Query("""
        SELECT c FROM CommercialVehicle c
        WHERE 
            LOWER(c.seller) = LOWER(:email)
        AND 
            (:#{#filters == null || #filters['brand'] == null || #filters['brand'] == ''} = true OR LOWER(c.brand) LIKE LOWER(CONCAT('%', :#{#filters['brand']}, '%')))
        AND 
            (:#{#filters == null || #filters['model'] == null || #filters['model'] == ''} = true OR LOWER(c.model) LIKE LOWER(CONCAT('%', :#{#filters['model']}, '%')))
        AND 
            (:#{#filters == null || #filters['type'] == null || #filters['type'] == ''} = true OR LOWER(c.type) = LOWER(:#{#filters['type']}))
        AND 
            (:#{#filters == null || #filters['transmission'] == null || #filters['transmission'] == ''} = true OR LOWER(c.transmission) = LOWER(:#{#filters['transmission']}))
        AND 
            (:#{#filters == null || #filters['conditionType'] == null || #filters['conditionType'] == ''} = true OR LOWER(c.conditionType) = LOWER(:#{#filters['conditionType']}))
        AND 
            (:#{#filters == null || #filters['bodyType'] == null || #filters['bodyType'] == ''} = true OR LOWER(c.bodyType) = LOWER(:#{#filters['bodyType']}))
        AND 
            (:#{#filters == null || #filters['fuelType'] == null || #filters['fuelType'] == ''} = true OR LOWER(c.fuelType) = LOWER(:#{#filters['fuelType']}))
        AND 
            (:#{#filters == null || #filters['yearOfManufacture'] == null || #filters['yearOfManufacture'] == ''} = true OR c.yearOfManufacture = :#{#filters['yearOfManufacture']})
        AND 
            (:#{#filters == null || #filters['location'] == null || #filters['location'] == ''} = true OR LOWER(c.location) LIKE LOWER(CONCAT('%', :#{#filters['location']}, '%')))
        AND 
            (:#{#filters == null || #filters['color'] == null || #filters['color'] == ''} = true OR LOWER(c.color) = LOWER(:#{#filters['color']}))
        AND 
            (:#{#filters == null || #filters['engineType'] == null || #filters['engineType'] == ''} = true OR LOWER(c.engineType) = LOWER(:#{#filters['engineType']}))
        AND 
            (:#{#filters == null || #filters['ownerType'] == null || #filters['ownerType'] == ''} = true OR LOWER(c.ownerType) = LOWER(:#{#filters['ownerType']}))
        AND 
            (:#{#filters == null || #filters['status'] == null || #filters['status'] == ''} = true OR LOWER(c.status) = LOWER(:#{#filters['status']}))
        AND 
            (:#{#filters == null || #filters['minPrice'] == null || #filters['minPrice'] == ''} = true OR c.priceKes >= CAST(:#{#filters['minPrice']} AS integer))
        AND 
            (:#{#filters == null || #filters['maxPrice'] == null || #filters['maxPrice'] == ''} = true OR c.priceKes <= CAST(:#{#filters['maxPrice']} AS integer))
        AND 
            (:#{#filters == null || #filters['minMileage'] == null || #filters['minMileage'] == ''} = true OR c.mileageKm >= CAST(:#{#filters['minMileage']} AS integer))
        AND 
            (:#{#filters == null || #filters['maxMileage'] == null || #filters['maxMileage'] == ''} = true OR c.mileageKm <= CAST(:#{#filters['maxMileage']} AS integer))
        AND 
            (:#{#filters == null || #filters['minYear'] == null || #filters['minYear'] == ''} = true OR c.yearOfManufacture >= CAST(:#{#filters['minYear']} AS integer))
        AND 
            (:#{#filters == null || #filters['maxYear'] == null || #filters['maxYear'] == ''} = true OR c.yearOfManufacture <= CAST(:#{#filters['maxYear']} AS integer))
        """)
    Page<CommercialVehicle> searchBySeller(@Param("filters") Map<String, String> filters, Pageable pageable, @Param("email") String email);

    // ------------------- Admin/Public search with all filters -------------------
    @Query("""
        SELECT c FROM CommercialVehicle c
        WHERE 
            (:#{#filters == null || #filters['brand'] == null || #filters['brand'] == ''} = true OR LOWER(c.brand) LIKE LOWER(CONCAT('%', :#{#filters['brand']}, '%')))
        AND 
            (:#{#filters == null || #filters['model'] == null || #filters['model'] == ''} = true OR LOWER(c.model) LIKE LOWER(CONCAT('%', :#{#filters['model']}, '%')))
        AND 
            (:#{#filters == null || #filters['type'] == null || #filters['type'] == ''} = true OR LOWER(c.type) = LOWER(:#{#filters['type']}))
        AND 
            (:#{#filters == null || #filters['transmission'] == null || #filters['transmission'] == ''} = true OR LOWER(c.transmission) = LOWER(:#{#filters['transmission']}))
        AND 
            (:#{#filters == null || #filters['conditionType'] == null || #filters['conditionType'] == ''} = true OR LOWER(c.conditionType) = LOWER(:#{#filters['conditionType']}))
        AND 
            (:#{#filters == null || #filters['bodyType'] == null || #filters['bodyType'] == ''} = true OR LOWER(c.bodyType) = LOWER(:#{#filters['bodyType']}))
        AND 
            (:#{#filters == null || #filters['fuelType'] == null || #filters['fuelType'] == ''} = true OR LOWER(c.fuelType) = LOWER(:#{#filters['fuelType']}))
        AND 
            (:#{#filters == null || #filters['yearOfManufacture'] == null || #filters['yearOfManufacture'] == ''} = true OR c.yearOfManufacture = :#{#filters['yearOfManufacture']})
        AND 
            (:#{#filters == null || #filters['location'] == null || #filters['location'] == ''} = true OR LOWER(c.location) LIKE LOWER(CONCAT('%', :#{#filters['location']}, '%')))
        AND 
            (:#{#filters == null || #filters['color'] == null || #filters['color'] == ''} = true OR LOWER(c.color) = LOWER(:#{#filters['color']}))
        AND 
            (:#{#filters == null || #filters['engineType'] == null || #filters['engineType'] == ''} = true OR LOWER(c.engineType) = LOWER(:#{#filters['engineType']}))
        AND 
            (:#{#filters == null || #filters['ownerType'] == null || #filters['ownerType'] == ''} = true OR LOWER(c.ownerType) = LOWER(:#{#filters['ownerType']}))
        AND 
            (:#{#filters == null || #filters['status'] == null || #filters['status'] == ''} = true OR LOWER(c.status) = LOWER(:#{#filters['status']}))
        AND 
            (:#{#filters == null || #filters['minPrice'] == null || #filters['minPrice'] == ''} = true OR c.priceKes >= CAST(:#{#filters['minPrice']} AS integer))
        AND 
            (:#{#filters == null || #filters['maxPrice'] == null || #filters['maxPrice'] == ''} = true OR c.priceKes <= CAST(:#{#filters['maxPrice']} AS integer))
        AND 
            (:#{#filters == null || #filters['minMileage'] == null || #filters['minMileage'] == ''} = true OR c.mileageKm >= CAST(:#{#filters['minMileage']} AS integer))
        AND 
            (:#{#filters == null || #filters['maxMileage'] == null || #filters['maxMileage'] == ''} = true OR c.mileageKm <= CAST(:#{#filters['maxMileage']} AS integer))
        AND 
            (:#{#filters == null || #filters['minYear'] == null || #filters['minYear'] == ''} = true OR c.yearOfManufacture >= CAST(:#{#filters['minYear']} AS integer))
        AND 
            (:#{#filters == null || #filters['maxYear'] == null || #filters['maxYear'] == ''} = true OR c.yearOfManufacture <= CAST(:#{#filters['maxYear']} AS integer))
        """)
    Page<CommercialVehicle> search(@Param("filters") Map<String, String> filters, Pageable pageable);
}