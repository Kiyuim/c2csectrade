package lut.cn.c2cplatform.controller;

import lut.cn.c2cplatform.dto.ProductCreateDTO;
import lut.cn.c2cplatform.dto.ProductDTO;
import lut.cn.c2cplatform.entity.Product;
import lut.cn.c2cplatform.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lut.cn.c2cplatform.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    @Autowired
    private ProductService productService;
    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createProduct(
            @RequestPart("productData") String productDataJson,
            @RequestPart("files") List<MultipartFile> files,
            Authentication authentication) {
        try {
            ProductCreateDTO createDTO = objectMapper.readValue(productDataJson, ProductCreateDTO.class);

            // 安全地获取用户ID
            Long userId = null;
            Object principal = authentication.getPrincipal();

            if (principal instanceof UserDetailsImpl) {
                userId = ((UserDetailsImpl) principal).getId();
            } else if (principal instanceof org.springframework.security.core.userdetails.User) {
                // 从用户名获取用户ID
                String username = ((org.springframework.security.core.userdetails.User) principal).getUsername();
                var user = productService.getUserByUsername(username);
                if (user != null) {
                    userId = user.getId();
                }
            } else if (principal instanceof String) {
                // 直接是用户名字符串
                var user = productService.getUserByUsername((String) principal);
                if (user != null) {
                    userId = user.getId();
                }
            }

            if (userId == null) {
                return new ResponseEntity<>("无法获取用户信息", HttpStatus.UNAUTHORIZED);
            }

            var createdProduct = productService.createProduct(createDTO, files, userId);
            return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
        } catch (ClassCastException e) {
            return new ResponseEntity<>("用户认证类型错误: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return new ResponseEntity<>("Error creating product: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public ResponseEntity<List<ProductDTO>> listProducts() {
        try {
            List<Product> products = productService.listAllProducts();
            List<ProductDTO> productDTOs = productService.convertToDTOList(products);
            return ResponseEntity.ok(productDTOs);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable Long id) {
        try {
            ProductDTO productDTO = productService.getProductDTOById(id);
            if (productDTO == null) {
                return new ResponseEntity<>("Product not found", HttpStatus.NOT_FOUND);
            }
            return ResponseEntity.ok(productDTO);
        } catch (Exception e) {
            return new ResponseEntity<>("Error fetching product: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);
            return ResponseEntity.ok("Product deleted successfully");
        } catch (Exception e) {
            return new ResponseEntity<>("Error deleting product: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/my-products")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMyProducts(Authentication authentication) {
        try {
            Long userId = extractUserId(authentication);
            if (userId == null) {
                return new ResponseEntity<>("无法获取用户信息", HttpStatus.UNAUTHORIZED);
            }

            List<Product> products = productService.getProductsByUserId(userId);
            List<ProductDTO> productDTOs = productService.convertToDTOList(products);
            return ResponseEntity.ok(productDTOs);
        } catch (Exception e) {
            return new ResponseEntity<>("Error fetching products: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateProduct(
            @PathVariable Long id,
            @RequestPart("productData") String productDataJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            Authentication authentication) {
        try {
            Long userId = extractUserId(authentication);
            if (userId == null) {
                return new ResponseEntity<>("无法获取用户信息", HttpStatus.UNAUTHORIZED);
            }

            // Verify ownership
            Product existingProduct = productService.getProductById(id);
            if (existingProduct == null) {
                return new ResponseEntity<>("商品不存在", HttpStatus.NOT_FOUND);
            }
            if (!existingProduct.getUserId().equals(userId)) {
                return new ResponseEntity<>("无权限修改此商品", HttpStatus.FORBIDDEN);
            }

            ProductCreateDTO updateDTO = objectMapper.readValue(productDataJson, ProductCreateDTO.class);
            Product updatedProduct = productService.updateProduct(id, updateDTO, files);
            return ResponseEntity.ok(updatedProduct);
        } catch (Exception e) {
            return new ResponseEntity<>("Error updating product: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Long extractUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetailsImpl) {
            return ((UserDetailsImpl) principal).getId();
        } else if (principal instanceof org.springframework.security.core.userdetails.User) {
            String username = ((org.springframework.security.core.userdetails.User) principal).getUsername();
            var user = productService.getUserByUsername(username);
            return user != null ? user.getId() : null;
        } else if (principal instanceof String) {
            var user = productService.getUserByUsername((String) principal);
            return user != null ? user.getId() : null;
        }
        return null;
    }
}
