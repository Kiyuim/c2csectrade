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
            // 事务外：先上传文件到MinIO（如果有文件的话）
            if (files != null && !files.isEmpty()) {
                for (MultipartFile file : files) {
                    if (!file.isEmpty()) {
                        String url = fileStorageService.uploadFile(file);
                        uploadedUrls.add(url);
                    }
                }
            }

            // 事务内：保存到数据库
            Product product = saveProductWithMedia(dto, uploadedUrls, userId);

            // 发布事件：通知ES进行索引（异步处理，避免循环依赖）
            eventPublisher.publishEvent(new ProductCreatedEvent(this, product));

            return product;
                } catch (Exception e) {
                    System.err.println("创建商品失败: " + e.getMessage());
                    e.printStackTrace();
        
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
                            System.err.println("清理上传文件失败: " + deleteException.getMessage());
        
                        }
                    }
                    throw new RuntimeException("创建商品失败: " + e.getMessage(), e);
                }
    }

    @Transactional
    protected Product saveProductWithMedia(ProductCreateDTO dto, List<String> urls, Long userId) {
        System.out.println("开始保存商品，用户ID: " + userId);
        System.out.println("商品信息: name=" + dto.getName() + ", category=" + dto.getCategory() + ", price=" + dto.getPrice());

        Product product = new Product();
        product.setUserId(userId);
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setConditionLevel(dto.getConditionLevel());
        product.setLocation(dto.getLocation());
        product.setCategory(dto.getCategory());
        product.setStock(dto.getStock() != null ? dto.getStock() : 1);
        product.setStatus(1);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());

        System.out.println("插入商品基本信息...");
        int insertResult = productMapper.insert(product);
        if (insertResult == 0) {
            throw new RuntimeException("插入商品基本信息失败");
        }
        System.out.println("商品插入成功，ID: " + product.getId());

        List<ProductMedia> mediaList = new ArrayList<>();
        if (urls != null && !urls.isEmpty()) {
            int sortOrder = 0;
            for (String url : urls) {
                System.out.println("保存媒体文件: " + url);
                ProductMedia media = new ProductMedia();

                // 创建Product引用对象
                Product productRef = new Product();
                productRef.setId(product.getId());
                media.setProduct(productRef);

                media.setUrl(url);
                media.setMediaType(url.matches("(?i).*\\.(mp4|mov|avi|wmv|flv|mkv)$") ? 2 : 1);
                media.setSortOrder(sortOrder++);

                int mediaInsertResult = productMediaMapper.insert(media);
                if (mediaInsertResult > 0) {
                    mediaList.add(media);
                    System.out.println("媒体保存成功，ID: " + media.getId());
                } else {
                    throw new RuntimeException("插入媒体记录失败");
                }
            }
        } else {
            System.out.println("无媒体文件需要保存");
        }

        product.setMedia(mediaList);
        System.out.println("商品保存完成");
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
            List<ProductMedia> media = productMediaMapper.selectByProductId(product.getId());
            product.setMedia(media);
        }
        return products;
    }

    /**
     * 带筛选条件的商品查询
     */
    public List<Product> listProductsWithFilters(String keyword, java.math.BigDecimal minPrice,
                                                   java.math.BigDecimal maxPrice, Integer conditionLevel,
                                                   String location, String category) {
        List<Product> products = productMapper.selectWithFilters(keyword, minPrice, maxPrice, conditionLevel, location, category);
        // 为每个商品加载媒体
        for (Product product : products) {
            List<ProductMedia> media = productMediaMapper.selectByProductId(product.getId());
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
        dto.setId(product.getId()); // id已经是Long类型，直接使用
        dto.setUserId(product.getUserId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStock(product.getStock());
        dto.setConditionLevel(product.getConditionLevel());
        dto.setCategory(product.getCategory()); // 新增：商品分类
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
                dto.setAvatarUrl(user.getAvatarUrl()); // 新增：头像URL
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
                    media.getId(), // id已经是Long类型
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

    public void delistProduct(Long id) {
        Product product = productMapper.selectById(id);
        if (product != null) {
            product.setStatus(0);
            productMapper.update(product);
        }
    }

    /**
     * Get products by user ID
     */
    public List<Product> getProductsByUserId(Long userId) {
        List<Product> products = productMapper.selectByUserId(userId);
        // Load media for each product
        for (Product product : products) {
            List<ProductMedia> media = productMediaMapper.selectByProductId(product.getId());
            product.setMedia(media);
        }
        return products;
    }

    /**
     * Update product
     */
    @Transactional
    public Product updateProduct(Long id, ProductCreateDTO dto, List<MultipartFile> files) {
        try {
            System.out.println("开始更新商品，ID: " + id);

            Product product = productMapper.selectById(id);
            if (product == null) {
                throw new IllegalArgumentException("商品不存在");
            }

            // Update basic info
            product.setName(dto.getName());
            product.setDescription(dto.getDescription());
            product.setPrice(dto.getPrice());
            product.setCategory(dto.getCategory());
            product.setConditionLevel(dto.getConditionLevel());
            product.setLocation(dto.getLocation());
            product.setStock(dto.getStock() != null ? dto.getStock() : product.getStock());
            product.setUpdatedAt(LocalDateTime.now());

            System.out.println("更新商品基本信息...");
            int updateResult = productMapper.update(product);
            if (updateResult == 0) {
                throw new RuntimeException("更新商品基本信息失败");
            }

            // If new files provided, replace all media
            if (files != null && !files.isEmpty()) {
                System.out.println("处理新上传的文件，共 " + files.size() + " 个");

                // Delete old media from database (keep files in MinIO for now)
                List<ProductMedia> oldMedia = productMediaMapper.selectByProductId(id);
                System.out.println("删除旧媒体记录，共 " + oldMedia.size() + " 个");

                for (ProductMedia media : oldMedia) {
                    try {
                        productMediaMapper.deleteById(media.getId()); // id已经是Long类型
                    } catch (Exception e) {
                        System.err.println("删除旧媒体记录失败: " + e.getMessage());
                    }
                }

                // Upload and save new media
                List<ProductMedia> newMediaList = new ArrayList<>();
                int sortOrder = 0;

                for (MultipartFile file : files) {
                    try {
                        System.out.println("上传文件: " + file.getOriginalFilename());
                        String url = fileStorageService.uploadFile(file);

                        ProductMedia media = new ProductMedia();

                        // 直接使用productId
                        Product productRef = new Product();
                        productRef.setId(id); // 现在id是Long类型，直接使用
                        media.setProduct(productRef);

                        media.setUrl(url);

                        // 判断文件类型
                        String filename = file.getOriginalFilename();
                        if (filename != null && filename.matches("(?i).*\\.(mp4|mov|avi|wmv|flv|mkv)$")) {
                            media.setMediaType(2); // 视频
                        } else {
                            media.setMediaType(1); // 图片
                        }

                        media.setSortOrder(sortOrder++);

                        int insertResult = productMediaMapper.insert(media);
                        if (insertResult > 0) {
                            newMediaList.add(media);
                            System.out.println("文件上传并保存成功: " + url);
                        } else {
                            throw new RuntimeException("数据库插入媒体记录失败");
                        }
                    } catch (Exception e) {
                        System.err.println("上传文件失败: " + e.getMessage());
                        e.printStackTrace();
                        throw new RuntimeException("文件上传失败: " + file.getOriginalFilename() + ", 原因: " + e.getMessage());
                    }
                }
                product.setMedia(newMediaList);
            } else {
                // Keep existing media
                System.out.println("保留原有媒体文件");
                List<ProductMedia> media = productMediaMapper.selectByProductId(id);
                product.setMedia(media);
            }

            System.out.println("商品更新成功");
            return product;

        } catch (IllegalArgumentException e) {
            System.err.println("参数错误: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("更新商品失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("更新商品失败: " + e.getMessage(), e);
        }
    }

    /**
     * Get user by username
     */
    public User getUserByUsername(String username) {
        return userMapper.selectByUsername(username);
    }
}
