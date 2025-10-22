package lut.cn.c2cplatform.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class Product implements Serializable {
    private Long id;  // 修改为Long以匹配数据库BIGINT类型
    private Long userId;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer conditionLevel;
    private String location; // 新增：位置
    private String category; // 新增：商品分类
    private int stock;
    private int status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ProductMedia> media;
}
