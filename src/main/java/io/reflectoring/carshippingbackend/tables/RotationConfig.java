package io.reflectoring.carshippingbackend.tables;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "rotation_config")
@Data
public class RotationConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String configKey;

    private String configValue;

    private LocalDateTime lastUpdated;

    // Constants for config keys
    public static final String CURRENT_IMAGE_INDEX = "current_image_index";
    public static final String LAST_ROTATION_TIME = "last_rotation_time";
    public static final String ROTATION_INTERVAL_HOURS = "rotation_interval_hours";

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}
