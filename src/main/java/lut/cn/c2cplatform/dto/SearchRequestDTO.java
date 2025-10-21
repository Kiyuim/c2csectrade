package lut.cn.c2cplatform.dto;

import java.math.BigDecimal;

public class SearchRequestDTO {
    private String keyword;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer conditionLevel;
    private String location;
    private Integer page = 0;
    private Integer size = 20;

    // Getters and Setters
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }

    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }

    public Integer getConditionLevel() { return conditionLevel; }
    public void setConditionLevel(Integer conditionLevel) { this.conditionLevel = conditionLevel; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
}

