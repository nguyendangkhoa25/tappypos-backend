package com.tappy.pos.controller.product;

import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.product.*;
import com.tappy.pos.service.product.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.tappy.pos.annotation.RequiresFeature;

@Slf4j
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@RequiresFeature("PRODUCT")
public class ProductController {

    private final ProductService productService;

    /**
     * GET /api/products/summary
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ProductSummaryDTO>> getSummary() {
        return ResponseEntity.ok(ApiResponse.success(productService.getSummary(), "Summary retrieved"));
    }

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
            @RequestParam(required = false) Long categoryId,
            Pageable pageable) {
        log.info("GET /api/products - Get all products, status: {}, categoryId: {}, page: {}", status, categoryId, pageable.getPageNumber());
        Page<ProductDTO> products = productService.getAllProducts(status, categoryId, pageable);
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

    /**
     * PATCH /api/products/{id}/visibility
     * Set product visibility (active/inactive)
     */
    @PatchMapping("/{id}/visibility")
    public ResponseEntity<ApiResponse<Void>> setVisibility(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, Boolean> body) {
        log.info("Endpoint: PATCH /products/{}/visibility", id);
        Boolean active = body.get("active");
        if (active == null) return ResponseEntity.badRequest().body(ApiResponse.error("BAD_REQUEST", "active required"));
        productService.setVisibility(id, active);
        return ResponseEntity.ok(ApiResponse.success(null, "Visibility updated"));
    }

    /**
     * Barcode lookup — shop inventory first, then master catalog
     * GET /api/products/barcode/{barcode}
     */
    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<ApiResponse<BarcodeLookupResult>> lookupBarcode(
            @PathVariable String barcode) {
        log.info("GET /api/products/barcode/{} - Barcode lookup", barcode);
        BarcodeLookupResult result = productService.lookupByBarcode(barcode);
        return ResponseEntity.ok(ApiResponse.success(result, "Barcode lookup completed"));
    }
}

