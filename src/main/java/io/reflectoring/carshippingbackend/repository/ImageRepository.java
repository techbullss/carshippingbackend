package io.reflectoring.carshippingbackend.repository;


import io.reflectoring.carshippingbackend.tables.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ImageRepository extends JpaRepository<Image, String> {
    List<Image> findAllByOrderByUploadedAtDesc();

    @Query("SELECT i FROM Image i WHERE i.active = true")
    Optional<Image> findActiveImage();

    long count();
}
