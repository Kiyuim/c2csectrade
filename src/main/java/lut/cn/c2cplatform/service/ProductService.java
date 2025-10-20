package lut.cn.c2cplatform.service;

import lut.cn.c2cplatform.dto.ProductCreateDTO;
import lut.cn.c2cplatform.entity.Product;
import lut.cn.c2cplatform.entity.ProductMedia;
import lut.cn.c2cplatform.mapper.ProductMapper;
import lut.cn.c2cplatform.mapper.ProductMediaMapper;
import lut.cn.c2cplatform.config.MinioProperties;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Transactional
    public Product createProduct(ProductCreateDTO dto, List<MultipartFile> files, Long userId) {
        List<String> uploadedUrls = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                String url = fileStorageService.uploadFile(file);
                uploadedUrls.add(url);
            }
            return saveProductWithMedia(dto, uploadedUrls, userId);
        } catch (Exception e) {
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

    protected Product saveProductWithMedia(ProductCreateDTO dto, List<String> urls, Long userId) {
        Product product = new Product();
        product.setUserId(userId);
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setConditionLevel(dto.getConditionLevel());
        product.setStock(1);
        product.setStatus(1);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());

        // Insert product (MyBatis insert configured with useGeneratedKeys will populate product.id)
        productMapper.insert(product);

        List<ProductMedia> mediaList = new ArrayList<>();
        for (String url : urls) {
            ProductMedia media = new ProductMedia();
            media.setProduct(product);
            media.setUrl(url);
            media.setMediaType(url.matches(".*\\.(mp4|mov|avi)$") ? 2 : 1);
            productMediaMapper.insert(media);
            mediaList.add(media);
        }

        product.setMedia(mediaList);
        // Optionally update product if media relationship needs to be stored on product side
        productMapper.update(product);
        return product;
    }
}
