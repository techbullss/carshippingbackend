package io.reflectoring.carshippingbackend.services;

import io.reflectoring.carshippingbackend.DTO.SellerStatsDTO;
import io.reflectoring.carshippingbackend.repository.CarRepository;
import io.reflectoring.carshippingbackend.repository.CommercialVehicleRepository;
import io.reflectoring.carshippingbackend.repository.MotorcycleRepository;
import io.reflectoring.carshippingbackend.repository.ReviewRepositorySeller;
import io.reflectoring.carshippingbackend.tables.User;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor


public class SellerStatsService {

    private final CarRepository carRepository;
    private final ReviewRepositorySeller reviewRepository;
    private final UserService userService;
    private final MotorcycleRepository motorcyclerespository;
    private final CommercialVehicleRepository commercialvehicleresipository;

    public SellerStatsDTO getSellerStats(String email) {

        User seller = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        long listings = carRepository.countByPostedBy(email);

        Double rating = reviewRepository.getAverageRating(seller.getId());
        Long reviews = reviewRepository.getReviewCount(seller.getId());

        return SellerStatsDTO.builder()
                .totalListings(listings)
                .rating(rating != null ? rating : 0)
                .reviewCount(reviews != null ? reviews : 0)
                .build();
    }
    public SellerStatsDTO getSellerStatsCommercial(String email) {

        User seller = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        long listings = commercialvehicleresipository.countBySeller(email);

        Double rating = reviewRepository.getAverageRating(seller.getId());
        Long reviews = reviewRepository.getReviewCount(seller.getId());

        return SellerStatsDTO.builder()
                .totalListings(listings)
                .rating(rating != null ? rating : 0)
                .reviewCount(reviews != null ? reviews : 0)
                .build();
    }
    public SellerStatsDTO getSellerStatsMotorcycle(String email) {

        User seller = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        long listings = motorcyclerespository.countByOwner(email);

        Double rating = reviewRepository.getAverageRating(seller.getId());
        Long reviews = reviewRepository.getReviewCount(seller.getId());

        return SellerStatsDTO.builder()
                .totalListings(listings)
                .rating(rating != null ? rating : 0)
                .reviewCount(reviews != null ? reviews : 0)
                .build();
    }
}