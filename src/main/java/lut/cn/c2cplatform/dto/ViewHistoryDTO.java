package lut.cn.c2cplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for view history tracking
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ViewHistoryDTO {
    private Long productId;
    private Long timestamp;
}

