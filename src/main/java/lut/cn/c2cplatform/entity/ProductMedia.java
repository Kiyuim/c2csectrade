package lut.cn.c2cplatform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;

@Data
public class ProductMedia implements Serializable {
    private Integer id;
    @JsonIgnore
    private Product product;
    private String url;
    // 1 = image, 2 = video
    private int mediaType;
    private Integer sortOrder;
}
