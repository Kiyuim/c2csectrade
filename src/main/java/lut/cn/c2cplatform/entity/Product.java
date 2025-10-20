package lut.cn.c2cplatform.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class Product implements Serializable {
    private Integer id;
    private Long userId;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer conditionLevel;
    private int stock;
    private int status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ProductMedia> media;
}
