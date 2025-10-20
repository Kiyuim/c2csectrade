package lut.cn.c2cplatform.dto;

import java.math.BigDecimal;

public class ProductCreateDTO {
    private String name;
    private String description;
    private BigDecimal price;
    private Integer conditionLevel;
    // 可根据实际需求扩展字段

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getConditionLevel() { return conditionLevel; }
    public void setConditionLevel(Integer conditionLevel) { this.conditionLevel = conditionLevel; }
}

