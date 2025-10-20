package lut.cn.c2cplatform.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class ProductMedia implements Serializable {
    private Integer id;
    private Product product;
    private String url;
    private int mediaType; // 0 for image, 1 for video
}
