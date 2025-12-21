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
     * <p>
     * Request Body:
     * - name: Product name (required)
     * - description: Product description (optional)
     * - price: Product price including tax (required)
     * - priceBeforeTax: Product price before tax (optional)
     * - tax: Tax amount or percentage (optional, defaults to 0)
     * - durationMinutes: Service duration in minutes (optional)
     * - commissionRate: Commission rate in percentage (optional, defaults to 0)
     * - quantity: Product quantity (optional, defaults to 0)
     * <p>
     * Examples:
     * - POST /api/products
     * {
     * "name": "Haircut",
     * "description": "Basic haircut service",
     * "price": 100000,
     * "priceBeforeTax": 100000,
     * "tax": 0,
     * "durationMinutes": 30,
     * "commissionRate": 10,
     * "quantity": 0
     * }
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProductDTO>> createProduct(
            @RequestBody ProductDTO productDTO) {
        log.info("Request: Create new product");
        ProductDTO createdProduct = productService.createProduct(productDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdProduct, "Product created successfully"));
    }

    /**
     * GET /api/products
     * Get all products with optional pagination, status filtering, search, and sorting
     * <p>
     * Query Parameters:
     * - page: Page number (0-based, default: 0)
     * - size: Page size (default: 20)
     * - search: Search term to search in name and description (optional)
     * - status: Filter by status - "active" or "inactive" (optional)
     * - sortBy: Sort by field name (e.g., "id", "createdAt", "name", "price", "commissionRate") (optional, default: "id")
     * - sortDirection: Sort direction "ASC" or "DESC" (optional, default: "DESC")
     * <p>
     * Examples:
     * - GET /api/products?page=0&size=20
     * - GET /api/products?page=0&size=20&search=haircut
     * - GET /api/products?page=0&size=20&status=active
     * - GET /api/products?page=0&size=20&sortBy=createdAt&sortDirection=DESC
     * - GET /api/products?page=0&size=20&search=massage&status=active&sortBy=price&sortDirection=ASC
     * - GET /api/products?page=0&size=20&sortBy=commissionRate&sortDirection=DESC
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
     * Get specific product details by ID
     * <p>
     * Path Parameters:
     * - productId: Product ID (required)
     * <p>
     * Returns:
     * - id: Product ID
     * - name: Product name
     * - description: Product description
     * - price: Product price (with tax)
     * - priceBeforeTax: Product price before tax
     * - tax: Tax amount
     * - durationMinutes: Service duration in minutes
     * - commissionRate: Commission rate in percentage
     * - quantity: Product quantity
     * - active: Active status
     * <p>
     * Examples:
     * - GET /api/products/20260000
     */
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductDTO>> getProduct(
            @PathVariable Long productId) {
        log.info("Request: Get product: {}", productId);
        ProductDTO product = productService.getProductById(productId);
        return ResponseEntity.ok(
                ApiResponse.success(product, "Product retrieved successfully")
        );
    }

    /**
     * PUT /api/products/{productId}
     * Update an existing product
     * <p>
     * Path Parameters:
     * - productId: Product ID (required)
     * <p>
     * Request Body:
     * - name: Product name (optional)
     * - description: Product description (optional)
     * - price: Product price including tax (optional)
     * - priceBeforeTax: Product price before tax (optional)
     * - tax: Tax amount or percentage (optional)
     * - durationMinutes: Service duration in minutes (optional)
     * - commissionRate: Commission rate in percentage (optional)
     * - quantity: Product quantity (optional)
     * <p>
     * Examples:
     * - PUT /api/products/20260000
     * {
     * "name": "Premium Haircut",
     * "price": 150000,
     * "priceBeforeTax": 150000,
     * "durationMinutes": 45,
     * "commissionRate": 15
     * }
     */
    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductDTO>> updateProduct(
            @PathVariable Long productId,
            @RequestBody ProductDTO productDTO) {
        log.info("Request: Update product: {}", productId);
        ProductDTO updatedProduct = productService.updateProduct(productId, productDTO);
        return ResponseEntity.ok(
                ApiResponse.success(updatedProduct, "Product updated successfully")
        );
    }

    /**
     * PATCH /api/products/{productId}/deactivate
     * Deactivate a product (soft delete)
     * <p>
     * Path Parameters:
     * - productId: Product ID (required)
     * <p>
     * Examples:
     * - PATCH /api/products/20260000/deactivate
     */
    @PatchMapping("/{productId}/deactivate")
    public ResponseEntity<ApiResponse<ProductDTO>> deactivateProduct(
            @PathVariable Long productId) {
        log.info("Request: Deactivate product: {}", productId);
        ProductDTO deactivatedProduct = productService.deactivateProduct(productId);
        return ResponseEntity.ok(
                ApiResponse.success(deactivatedProduct, "Product deactivated successfully")
        );
    }

    /**
     * PATCH /api/products/{productId}/activate
     * Activate a product
     * <p>
     * Path Parameters:
     * - productId: Product ID (required)
     * <p>
     * Examples:
     * - PATCH /api/products/20260000/activate
     */
    @PatchMapping("/{productId}/activate")
    public ResponseEntity<ApiResponse<ProductDTO>> activateProduct(
            @PathVariable Long productId) {
        log.info("Request: Activate product: {}", productId);
        ProductDTO activatedProduct = productService.activateProduct(productId);
        return ResponseEntity.ok(
                ApiResponse.success(activatedProduct, "Product activated successfully")
        );
    }

    /**
     * DELETE /api/products/{productId}
     * Delete (hard delete) a product
     * <p>
     * Path Parameters:
     * - productId: Product ID (required)
     * <p>
     * Examples:
     * - DELETE /api/products/20260000
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable Long productId) {
        log.info("Request: Delete product: {}", productId);
        productService.deleteProduct(productId);
        return ResponseEntity.ok(
                ApiResponse.success(null, "Product deleted successfully")
        );
    }
}
