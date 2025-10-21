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
            Long userId = ((UserDetailsImpl) authentication.getPrincipal()).getId();
            var createdProduct = productService.createProduct(createDTO, files, userId);
            return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
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
}
