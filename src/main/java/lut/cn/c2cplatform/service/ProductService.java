package lut.cn.c2cplatform.service;

import lut.cn.c2cplatform.dto.ProductCreateDTO;
import lut.cn.c2cplatform.dto.ProductDTO;
import lut.cn.c2cplatform.entity.Product;
import lut.cn.c2cplatform.entity.ProductMedia;
import lut.cn.c2cplatform.entity.User;
import lut.cn.c2cplatform.mapper.ProductMapper;
import lut.cn.c2cplatform.mapper.ProductMediaMapper;
import lut.cn.c2cplatform.mapper.UserMapper;
import lut.cn.c2cplatform.config.MinioProperties;
import lut.cn.c2cplatform.event.ProductCreatedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductMediaMapper productMediaMapper;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private MinioProperties minioProperties;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private UserMapper userMapper;

    public Product createProduct(ProductCreateDTO dto, List<MultipartFile> files, Long userId) {
        List<String> uploadedUrls = new ArrayList<>();
        try {
            // 事务外：先上传文件到MinIO
            for (MultipartFile file : files) {
                String url = fileStorageService.uploadFile(file);
                uploadedUrls.add(url);
            }
            // 事务内：保存到数据库
            Product product = saveProductWithMedia(dto, uploadedUrls, userId);

            // 发布事件：通知ES进行索引（异步处理，避免循环依赖）
            eventPublisher.publishEvent(new ProductCreatedEvent(this, product));

            return product;
        } catch (Exception e) {
            // 异常补偿：删除已上传的文件
            for (String url : uploadedUrls) {
                try {
                    String bucket = minioProperties.getBucketName();
                    int idx = url.indexOf(bucket + "/");
                    String objectName = idx >= 0 ? url.substring(idx + bucket.length() + 1) : null;
                    if (objectName != null) {
                        fileStorageService.deleteFile(objectName);
                    }
                } catch (Exception deleteException) {
                    // 可记录日志
                }
            }
            throw new RuntimeException("Failed to create product. Rolled back file uploads.", e);
        }
    }

    @Transactional
    protected Product saveProductWithMedia(ProductCreateDTO dto, List<String> urls, Long userId) {
        Product product = new Product();
        product.setUserId(userId);
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setConditionLevel(dto.getConditionLevel());
        product.setLocation(dto.getLocation());
        product.setStock(dto.getStock() != null ? dto.getStock() : 1);
        product.setStatus(1);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());

        // Insert product (MyBatis insert configured with useGeneratedKeys will populate product.id)
        productMapper.insert(product);

        List<ProductMedia> mediaList = new ArrayList<>();
        int sortOrder = 0;
        for (String url : urls) {
            ProductMedia media = new ProductMedia();
            media.setProduct(product);
            media.setUrl(url);
            media.setMediaType(url.matches("(?i).*\\.(mp4|mov|avi|wmv|flv|mkv)$") ? 2 : 1);
            media.setSortOrder(sortOrder++);
            productMediaMapper.insert(media);
            mediaList.add(media);
        }

        product.setMedia(mediaList);
        return product;
    }

    public Product getProductById(Long id) {
        Product product = productMapper.selectById(id);
        if (product != null) {
            List<ProductMedia> media = productMediaMapper.selectByProductId(id);
            product.setMedia(media);
        }
        return product;
    }

    public List<Product> listAllProducts() {
        List<Product> products = productMapper.selectAll();
        // 为每个商品加载媒体和用户信息
        for (Product product : products) {
            List<ProductMedia> media = productMediaMapper.selectByProductId(Long.valueOf(product.getId()));
            product.setMedia(media);
        }
        return products;
    }

    /**
     * Convert Product entity to ProductDTO
     */
    public ProductDTO convertToDTO(Product product) {
        if (product == null) {
            return null;
        }

        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId() != null ? Long.valueOf(product.getId()) : null);
        dto.setUserId(product.getUserId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStock(product.getStock());
        dto.setConditionLevel(product.getConditionLevel());
        dto.setLocation(product.getLocation());
        dto.setStatus(product.getStatus());
        dto.setCreateTime(product.getCreatedAt());
        dto.setUpdateTime(product.getUpdatedAt());

        // Get user info
        if (product.getUserId() != null) {
            User user = userMapper.selectById(product.getUserId());
            if (user != null) {
                dto.setUsername(user.getUsername());
                dto.setDisplayName(user.getDisplayName());
            }
        }

        // Convert media list
        if (product.getMedia() != null && !product.getMedia().isEmpty()) {
            List<String> imageUrls = new ArrayList<>();
            List<String> videoUrls = new ArrayList<>();
            List<ProductDTO.MediaItem> mediaItems = new ArrayList<>();

            for (ProductMedia media : product.getMedia()) {
                // 添加到media列表（用于前端多图展示）
                ProductDTO.MediaItem mediaItem = new ProductDTO.MediaItem(
                    media.getId() != null ? media.getId().longValue() : null,
                    media.getUrl(),
                    media.getMediaType(),
                    media.getSortOrder()
                );
                mediaItems.add(mediaItem);

                // 同时添加到imageUrls或videoUrls（向后兼容）
                if (media.getMediaType() == 1) {
                    imageUrls.add(media.getUrl());
                } else if (media.getMediaType() == 2) {
                    videoUrls.add(media.getUrl());
                }
            }

            dto.setMedia(mediaItems); // 设置media字段
            dto.setImageUrls(imageUrls);
            dto.setVideoUrls(videoUrls);

            // Set cover image as first image
            if (!imageUrls.isEmpty()) {
                dto.setCoverImage(imageUrls.get(0));
            }
        }

        return dto;
    }

    /**
     * Convert Product entity by ID to ProductDTO
     */
    public ProductDTO getProductDTOById(Long id) {
        Product product = getProductById(id);
        return convertToDTO(product);
    }

    /**
     * Convert list of Products to list of ProductDTOs
     */
    public List<ProductDTO> convertToDTOList(List<Product> products) {
        if (products == null) {
            return new ArrayList<>();
        }
        return products.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public void deleteProduct(Long id) {
        productMapper.deleteById(id);
    }

    /**
     * Get user by username
     */
    public User getUserByUsername(String username) {
        return userMapper.selectByUsername(username);
    }
}
