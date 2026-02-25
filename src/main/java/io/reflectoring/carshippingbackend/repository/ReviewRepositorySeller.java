package io.reflectoring.carshippingbackend.repository;

import io.reflectoring.carshippingbackend.tables.ReviewSeller;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepositorySeller extends JpaRepository<ReviewSeller, Long> {

    List<ReviewSeller> findBySellerIdAndApprovedTrue(Long sellerId);

    List<ReviewSeller> findByVehicleIdAndApprovedTrue(Long vehicleId);

}
