package io.reflectoring.carshippingbackend.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SellerStatsDTO {

    private double rating;
    private long reviewCount;
    private long totalListings;
}
