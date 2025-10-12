package io.reflectoring.carshippingbackend.repository;
import io.reflectoring.carshippingbackend.tables.Container;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContainerRepository extends JpaRepository<Container, Long> {

    Page<Container> findByContainerNumberContainingIgnoreCaseOrTypeContainingIgnoreCase(
            String containerNumber, String type, Pageable pageable
    );

    Page<Container> findByStatusIgnoreCase(String status, Pageable pageable);
}
