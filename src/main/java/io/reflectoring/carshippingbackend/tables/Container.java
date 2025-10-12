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
@Table(name = "container")  // Explicit table name
public class Container {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "container_seq")
    @SequenceGenerator(name = "container_seq", sequenceName = "container_seq", allocationSize = 1)
    private Long id;

    @Column(name = "container_number")
    private String containerNumber;

    @Column(name = "container_size") // Renamed from "size" to avoid reserved word
    private String size;  // e.g. 20ft, 40ft

    private String type;   // e.g. Dry, Reefer

    private Double price;

    private String status; // e.g. Available, Sold

    // FIX: Add proper collection mapping for Oracle
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "container_images",
            joinColumns = @JoinColumn(name = "container_id")
    )
    @Column(name = "image_url")
    private List<String> imageUrls; // store uploaded file URLs
}