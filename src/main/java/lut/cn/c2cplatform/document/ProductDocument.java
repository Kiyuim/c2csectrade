package lut.cn.c2cplatform.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(indexName = "products")
public class ProductDocument {
    @Id
    private String id;

    @Field(type = FieldType.Long)
    private Long productId;

    @Field(type = FieldType.Long)
    private Long userId;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String name;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Integer)
    private Integer conditionLevel;

    @Field(type = FieldType.Keyword)
    private String location;

    @Field(type = FieldType.Integer)
    private Integer status;

    @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd'T'HH:mm:ss||yyyy-MM-dd HH:mm:ss||yyyy-MM-dd'T'HH:mm:ss.SSSX||yyyy-MM-dd'T'HH:mm:ss.SSS||yyyy-MM-dd||epoch_millis")
    private LocalDateTime createdAt;

    @Field(type = FieldType.Keyword)
    private String category;

    // Highlighting fields (not stored in ES, just for transport)
    private String highlightedName;
    private String highlightedDescription;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getConditionLevel() {
        return conditionLevel;
    }

    public void setConditionLevel(Integer conditionLevel) {
        this.conditionLevel = conditionLevel;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getHighlightedName() {
        return highlightedName;
    }

    public void setHighlightedName(String highlightedName) {
        this.highlightedName = highlightedName;
    }

    public String getHighlightedDescription() {
        return highlightedDescription;
    }

    public void setHighlightedDescription(String highlightedDescription) {
        this.highlightedDescription = highlightedDescription;
    }
}
