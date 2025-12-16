package io.reflectoring.carshippingbackend.repository;


import io.reflectoring.carshippingbackend.tables.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findAllByOrderByUploadedAtDesc();
    Optional<Image> findByActiveTrue();
    long count();
}