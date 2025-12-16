package io.reflectoring.carshippingbackend.repository;

import io.reflectoring.carshippingbackend.tables.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // For public display - get all reviews (no approval filter needed)
    Page<Review> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // For admin view - filter by approval status if needed
    Page<Review> findByApproved(Boolean approved, Pageable pageable);

    // Get average rating (now includes all reviews)
    @Query("SELECT AVG(r.rating) FROM Review r")
    Double getAverageRating();

    // Get total number of reviews
    @Query("SELECT COUNT(r) FROM Review r")
    Long getTotalReviews();

    // Get rating distribution for stats
    @Query("SELECT r.rating, COUNT(r) as count FROM Review r GROUP BY r.rating ORDER BY r.rating DESC")
    List<Object[]> getRatingDistribution();

    // Search reviews by client name or item name
    @Query("SELECT r FROM Review r WHERE " +
            "LOWER(r.clientName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(r.itemName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(r.comment) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Review> searchReviews(@Param("search") String search, Pageable pageable);
}