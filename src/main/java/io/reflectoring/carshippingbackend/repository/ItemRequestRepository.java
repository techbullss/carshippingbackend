package io.reflectoring.carshippingbackend.repository;

import io.reflectoring.carshippingbackend.tables.ItemRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ItemRequestRepository extends JpaRepository<ItemRequest, Long> {

    Page<ItemRequest> findByStatus(String status, Pageable pageable);

    Page<ItemRequest> findByClientEmail(String clientEmail, Pageable pageable);

    // Client requests with search
    @Query("SELECT r FROM ItemRequest r WHERE r.clientEmail = :email AND " +
            "(:status IS NULL OR r.status = :status) AND " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(r.itemName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(r.requestId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(r.category) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(r.clientName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<ItemRequest> findClientRequestsWithFilters(
            @Param("email") String email,
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable);

    // Admin requests with search
    @Query("SELECT r FROM ItemRequest r WHERE " +
            "(:status IS NULL OR r.status = :status) AND " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(r.itemName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(r.requestId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(r.category) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(r.clientName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(r.clientEmail) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<ItemRequest> findAllWithFilters(
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable);

    // Count by status
    long countByStatus(String status);
    Optional<ItemRequest> findByReviewToken(String reviewToken);
}