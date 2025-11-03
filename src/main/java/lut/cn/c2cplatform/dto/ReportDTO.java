package lut.cn.c2cplatform.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ReportDTO {
    private Long id;
    private Long productId;
    private String productName;
    private Long reporterId;
    private String reporterUsername;
    private String reporterDisplayName;
    private String reason;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

