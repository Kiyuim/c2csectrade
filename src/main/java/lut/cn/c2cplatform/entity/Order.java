package lut.cn.c2cplatform.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class Order {
    private Integer id;
    private Integer userId;
    private BigDecimal totalAmount;
    private String status;
    private String paymentMethod;
    private Date expireTime;
    private Date createdAt;
    private Date updatedAt;
}
