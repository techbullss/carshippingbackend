package io.reflectoring.carshippingbackend.repository;

import io.reflectoring.carshippingbackend.tables.ReviewSeller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReviewRepositorySeller extends JpaRepository<ReviewSeller, Long> {

    List<ReviewSeller> findBySellerIdAndApprovedTrue(Long sellerId);

    List<ReviewSeller> findByVehicleIdAndApprovedTrue(Long vehicleId);
    @Query("SELECT AVG(r.rating) FROM ReviewSeller r WHERE r.sellerId = :sellerId AND r.approved = true")
    Double getAverageRating(Long sellerId);

    @Query("SELECT COUNT(r) FROM ReviewSeller r WHERE r.sellerId = :sellerId AND r.approved = true")
    Long getReviewCount(Long sellerId);
}
