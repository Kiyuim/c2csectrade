package lut.cn.c2cplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductDTO {
    private Long id;
    private Long userId;
    private String username;
    private String displayName;
    private String avatarUrl; // 新增：卖家头像URL
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private Integer conditionLevel;
    private String location;
    private String category; // 新增：商品分类
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<String> imageUrls;
    private List<String> videoUrls;
    private String coverImage;
    private List<MediaItem> media; // 添加media字段以支持前端多图展示

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MediaItem {
        private Long id;
        private String url;
        private Integer mediaType; // 1=图片, 2=视频
        private Integer sortOrder;
    }
}
