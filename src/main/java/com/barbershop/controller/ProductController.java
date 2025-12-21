package com.barbershop.controller;

import com.barbershop.model.dto.ApiResponse;
import com.barbershop.model.dto.ProductDTO;
import com.barbershop.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * ProductController - REST API endpoints for product (service) management
 * Database routing is handled automatically by TenantContext and RoutingDataSource
 */
@RestController
@RequestMapping("/products")
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
     * Get all products with optional pagination, status filtering, and search
     *
     * Query Parameters:
     * - page: Page number (0-based, default: 0)
     * - size: Page size (default: 20)
     * - sort: Sort field (default: id,asc)
     * - sortBy: Sort by field name (e.g., "id", "createdAt", "name") (optional, default: "id")
     * - sortDirection: Sort direction "ASC" or "DESC" (optional, default: "DESC")
     * - status: Filter by status - "active" or "inactive" (optional)
     * - search: Search term to search in name and description (optional)
     *
     * Examples:
     * - GET /api/products?page=0&size=10
     * - GET /api/products?page=0&size=10&sortBy=createdAt&sortDirection=DESC
     * - GET /api/products?page=0&size=10&status=active
     * - GET /api/products?page=0&size=10&search=massage&sortBy=name&sortDirection=ASC
     * - GET /api/products?page=0&size=10&status=active&search=massage&sortBy=price&sortDirection=DESC
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductDTO>>> getAllProducts(
            @RequestParam(value = "search", required = false) String searchTerm,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "sortBy", required = false, defaultValue = "id") String sortBy,
            @RequestParam(value = "sortDirection", required = false, defaultValue = "DESC") String sortDirection,
            Pageable pageable) {
        log.info("Request: Get all products - search: {}, status: {}, sortBy: {}, sortDirection: {}, page: {}, size: {}",
                searchTerm, status, sortBy, sortDirection, pageable.getPageNumber(), pageable.getPageSize());

        // Convert status string to boolean (null, true for "active", false for "inactive")
        Boolean statusFilter = null;
        if (status != null && !status.trim().isEmpty()) {
            statusFilter = status.equalsIgnoreCase("active");
        }

        Page<ProductDTO> productsPage = productService.getAllProductsWithFilters(
                searchTerm, statusFilter, sortBy, sortDirection, pageable);
        log.info("Retrieved {} products from page {}", productsPage.getContent().size(), pageable.getPageNumber());
        return ResponseEntity.ok(
            ApiResponse.success(productsPage, "Products retrieved successfully")
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
    @PatchMapping("/{productId}/deactivate")
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
    @PatchMapping("/{productId}/activate")
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
