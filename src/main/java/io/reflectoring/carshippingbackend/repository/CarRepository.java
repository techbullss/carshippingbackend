package io.reflectoring.carshippingbackend.repository;
import io.reflectoring.carshippingbackend.tables.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CarRepository extends JpaRepository<Car, Long>, JpaSpecificationExecutor<Car> {
    @Query("SELECT c FROM Car c WHERE c.brand = :brand AND c.model = :model AND c.id != :excludeId")
    List<Car> findByMakeAndModelAndIdNot(
            @Param("brand") String make,
            @Param("model") String model,
            @Param("excludeId") Long excludeId);

}

