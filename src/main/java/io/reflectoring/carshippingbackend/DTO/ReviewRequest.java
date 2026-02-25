package io.reflectoring.carshippingbackend.DTO;

import lombok.Data;

@Data
public class ReviewRequest {

    private Long vehicleId;
    private Long sellerId;
    private Integer rating;
    private String comment;
    private String reviewerName;
}
