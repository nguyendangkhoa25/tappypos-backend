package com.knp.controller.product;

import com.knp.model.dto.ApiResponse;
import com.knp.model.dto.product.*;
import com.knp.service.product.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.knp.annotation.RequiresFeature;

@Slf4j
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@RequiresFeature("PRODUCT")
public class ProductController {

    private final ProductService productService;

    /**
     * Get all product types (specific path must come before /{id})
     * GET /api/products/types/all
     */
    @GetMapping("/types/all")
    public ResponseEntity<ApiResponse<List<ProductTypeDTO>>> getAllProductTypes() {
        log.info("GET /api/products/types/all - Get all product types");
        List<ProductTypeDTO> types = productService.getAllProductTypes();
        return ResponseEntity.ok(ApiResponse.success(types, "Product types retrieved successfully"));
    }

    /**
     * Get product type with attributes (specific path must come before /{id})
     * GET /api/products/types/{typeId}/attributes
     */
    @GetMapping("/types/{typeId}/attributes")
    public ResponseEntity<ApiResponse<ProductTypeWithAttributesDTO>> getProductTypeWithAttributes(
            @PathVariable Long typeId) {
        log.info("GET /api/products/types/{}/attributes - Get product type with attributes", typeId);
        ProductTypeWithAttributesDTO typeWithAttrs = productService.getProductTypeWithAttributes(typeId);
        return ResponseEntity.ok(ApiResponse.success(typeWithAttrs, "Product type attributes retrieved successfully"));
    }

    /**
     * Suggest a unique SKU based on product name and type code
     * GET /api/products/sku/suggest?name=...&typeCode=...
     */
    @GetMapping("/sku/suggest")
    public ResponseEntity<ApiResponse<String>> suggestSku(
            @RequestParam String name,
            @RequestParam String typeCode) {
        log.info("GET /api/products/sku/suggest - name: {}, typeCode: {}", name, typeCode);
        String sku = productService.generateSku(name, typeCode);
        return ResponseEntity.ok(ApiResponse.success(sku, "SKU generated successfully"));
    }

    /**
     * Search products (specific path must come before /{id})
     * GET /api/products/search?keyword=value
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<ProductDTO>>> searchProducts(
            @RequestParam String keyword,
            Pageable pageable) {
        log.info("GET /api/products/search - Search products by keyword: {}", keyword);
        Page<ProductDTO> products = productService.searchProducts(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(products, "Products retrieved successfully"));
    }

    /**
     * Get products by type (specific path must come before /{id})
     * GET /api/products/type/{typeId}
     */
    @GetMapping("/type/{typeId}")
    public ResponseEntity<ApiResponse<Page<ProductDTO>>> getProductsByType(
            @PathVariable Long typeId,
            Pageable pageable) {
        log.info("GET /api/products/type/{} - Get products by type", typeId);
        Page<ProductDTO> products = productService.getProductsByType(typeId, pageable);
        return ResponseEntity.ok(ApiResponse.success(products, "Products retrieved successfully"));
    }

    /**
     * Get all products with pagination
     * GET /api/products
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductDTO>>> getAllProducts(
            @RequestParam(defaultValue = "ACTIVE") String status,
            Pageable pageable) {
        log.info("GET /api/products - Get all products, status: {}, page: {}", status, pageable.getPageNumber());
        Page<ProductDTO> products = productService.getAllProducts(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(products, "Products retrieved successfully"));
    }

    /**
     * Create a new product
     * POST /api/products
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProductDTO>> createProduct(
            @RequestBody @Valid CreateProductRequest request) {
        log.info("POST /api/products - Create product: {}", request.getName());
        ProductDTO product = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(product, "Product created successfully"));
    }

    /**
     * Get product by ID (generic path must come last)
     * GET /api/products/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDTO>> getProductById(@PathVariable Long id) {
        log.info("GET /api/products/{} - Get product by id", id);
        ProductDTO product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(product, "Product retrieved successfully"));
    }

    /**
     * Update product
     * PUT /api/products/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDTO>> updateProduct(
            @PathVariable Long id,
            @RequestBody @Valid UpdateProductRequest request) {
        log.info("PUT /api/products/{} - Update product", id);
        ProductDTO product = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success(product, "Product updated successfully"));
    }

    /**
     * Delete product
     * DELETE /api/products/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        log.info("DELETE /api/products/{} - Delete product", id);
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Product deleted successfully"));
    }
}

