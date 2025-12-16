package io.reflectoring.carshippingbackend.repository;

import io.reflectoring.carshippingbackend.tables.RotationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RotationConfigRepository extends JpaRepository<RotationConfig, Long> {
    Optional<RotationConfig> findByConfigKey(String configKey);
}
