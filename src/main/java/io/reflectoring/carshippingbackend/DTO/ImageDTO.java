package io.reflectoring.carshippingbackend.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ImageDTO {
    private String id;
    private String fileName;
    private String originalName;
    private String url;
    private String fileType;
    private Long fileSize;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadedAt;

    private boolean active;
    private String formattedSize;
    private String uploadDateFormatted;
}

