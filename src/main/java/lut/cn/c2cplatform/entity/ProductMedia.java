package lut.cn.c2cplatform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;

@Data
public class ProductMedia implements Serializable {
    private Long id;  // 修改为Long以匹配数据库BIGINT类型
    @JsonIgnore
    private Product product;
    private String url;
    // 1 = image, 2 = video
    private int mediaType;
    private Integer sortOrder;
}
