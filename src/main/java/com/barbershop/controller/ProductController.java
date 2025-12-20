package com.barbershop.controller;

import com.barbershop.model.dto.ApiResponse;
import com.barbershop.model.dto.ProductDTO;
import com.barbershop.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * ProductController - REST API endpoints for product (service) management
 * Database routing is handled automatically by TenantContext and RoutingDataSource
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;

    /**
     * POST /api/products
     * Create a new product (service)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProductDTO>> createProduct(
            @RequestBody ProductDTO productDTO) {
        log.info("Request: Create new product");
        ProductDTO createdProduct = productService.createProduct(productDTO);
        log.info("Created product: {}", createdProduct.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(createdProduct, "Product created successfully"));
    }

    /**
     * GET /api/products
     * Get all active products
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getActiveProducts() {
        log.info("Request: Get active products");
        List<ProductDTO> products = productService.getActiveProducts();
        log.info("Retrieved {} active products", products.size());
        return ResponseEntity.ok(
            ApiResponse.success(products, "Active products retrieved successfully")
        );
    }

    /**
     * GET /api/products/all
     * Get all products (including inactive)
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getAllProducts() {
        log.info("Request: Get all products");
        List<ProductDTO> products = productService.getAllProducts();
        log.info("Retrieved {} products", products.size());
        return ResponseEntity.ok(
            ApiResponse.success(products, "All products retrieved successfully")
        );
    }

    /**
     * GET /api/products/{productId}
     * Get specific product details
     */
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductDTO>> getProduct(
            @PathVariable Long productId) {
        log.info("Request: Get product: {}", productId);
        ProductDTO product = productService.getProductById(productId);
        log.info("Retrieved product: {}", productId);
        return ResponseEntity.ok(
            ApiResponse.success(product, "Product retrieved successfully")
        );
    }

    /**
     * PUT /api/products/{productId}
     * Update a product
     */
    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductDTO>> updateProduct(
            @PathVariable Long productId,
            @RequestBody ProductDTO productDTO) {
        log.info("Request: Update product: {}", productId);
        ProductDTO updatedProduct = productService.updateProduct(productId, productDTO);
        log.info("Updated product: {}", productId);
        return ResponseEntity.ok(
            ApiResponse.success(updatedProduct, "Product updated successfully")
        );
    }

    /**
     * PUT /api/products/{productId}/deactivate
     * Deactivate a product
     */
    @PutMapping("/{productId}/deactivate")
    public ResponseEntity<ApiResponse<ProductDTO>> deactivateProduct(
            @PathVariable Long productId) {
        log.info("Request: Deactivate product: {}", productId);
        ProductDTO deactivatedProduct = productService.deactivateProduct(productId);
        log.info("Deactivated product: {}", productId);
        return ResponseEntity.ok(
            ApiResponse.success(deactivatedProduct, "Product deactivated successfully")
        );
    }

    /**
     * PUT /api/products/{productId}/activate
     * Activate a product
     */
    @PutMapping("/{productId}/activate")
    public ResponseEntity<ApiResponse<ProductDTO>> activateProduct(
            @PathVariable Long productId) {
        log.info("Request: Activate product: {}", productId);
        ProductDTO activatedProduct = productService.activateProduct(productId);
        log.info("Activated product: {}", productId);
        return ResponseEntity.ok(
            ApiResponse.success(activatedProduct, "Product activated successfully")
        );
    }

    /**
     * DELETE /api/products/{productId}
     * Delete a product
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable Long productId) {
        log.info("Request: Delete product: {}", productId);
        productService.deleteProduct(productId);
        log.info("Deleted product: {}", productId);
        return ResponseEntity.ok(
            ApiResponse.success(null, "Product deleted successfully")
        );
    }
}
