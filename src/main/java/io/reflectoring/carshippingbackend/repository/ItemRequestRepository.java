// ItemRequestRepository.java
package io.reflectoring.carshippingbackend.repository;

import io.reflectoring.carshippingbackend.tables.ItemRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRequestRepository extends JpaRepository<ItemRequest, Long> {

    Page<ItemRequest> findByStatus(String status, Pageable pageable);

    Page<ItemRequest> findByClientEmail(String clientEmail, Pageable pageable);

    @Query("SELECT r FROM ItemRequest r WHERE " +
            "(:status IS NULL OR r.status = :status) AND " +
            "(:search IS NULL OR LOWER(r.itemName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(r.clientName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<ItemRequest> searchRequests(
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable);
}