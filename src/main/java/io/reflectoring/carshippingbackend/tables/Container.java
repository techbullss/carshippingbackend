package io.reflectoring.carshippingbackend.tables;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Container {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String containerNumber;
    private String size;   // e.g. 20ft, 40ft
    private String type;   // e.g. Dry, Reefer
    private Double price;
    private String status; // e.g. Available, Sold

    @ElementCollection
    private List<String> imageUrls; // store uploaded file URLs
}

