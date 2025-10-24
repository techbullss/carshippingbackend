package io.reflectoring.carshippingbackend.repository;
import io.reflectoring.carshippingbackend.tables.Car;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;

public interface CarRepository extends JpaRepository<Car, Long>, JpaSpecificationExecutor<Car> {
    @Query("SELECT c FROM Car c WHERE c.brand = :brand AND c.model = :model AND c.id != :excludeId")
    List<Car> findByMakeAndModelAndIdNot(
            @Param("brand") String make,
            @Param("model") String model,
            @Param("excludeId") Long excludeId);
    @Query("SELECT new map(c.brand as name, COUNT(c) as count) FROM Car c GROUP BY c.brand ORDER BY c.brand")
    List<Map<String, Object>> findDistinctMakesWithCount();

    @Query("SELECT DISTINCT new map(c.model as name) FROM Car c WHERE c.brand = :make ORDER BY c.model")
    List<Map<String, Object>> findDistinctModelsByMake(@Param("make") String make);
    @Query("SELECT c FROM Car c")
    Page<Car> search(@Param("filters") Map<String, String> filters, Pageable pageable);



    @Query("SELECT c FROM Car c WHERE c.seller = :email")
    Page<Car> searchBySeller(@Param("filters") Map<String, String> filters, Pageable pageable, @Param("email") String email);



}

