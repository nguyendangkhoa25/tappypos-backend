package com.tappy.pos.controller.tenant;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.product.ProductSuggestionDTO;
import com.tappy.pos.model.dto.product.ProductSuggestionRequest;
import com.tappy.pos.service.product.ProductSuggestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Master-admin endpoints for managing the curated product-suggestion list used when
 * creating a new shop. Requires PRODUCT_SUGGESTION_MGMT feature (MASTER_TENANT only).
 */
@RestController
@RequestMapping("/product-suggestions")
@RequiredArgsConstructor
@Slf4j
@RequiresFeature("PRODUCT_SUGGESTION_MGMT")
public class ProductSuggestionController {

    private final ProductSuggestionService productSuggestionService;

    /**
     * GET /api/product-suggestions?name=&shopType=&productType=&page=0&size=20
     * Search / list suggestions (paginated).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductSuggestionDTO>>> search(
            @RequestParam(required = false, defaultValue = "") String name,
            @RequestParam(required = false, defaultValue = "") String shopType,
            @RequestParam(required = false, defaultValue = "") String productType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Request: Product suggestion search name='{}' shopType='{}' productType='{}' page={} size={}",
                name, shopType, productType, page, size);
        Page<ProductSuggestionDTO> result = productSuggestionService.search(name, shopType, productType, page, size);
        return ResponseEntity.ok(ApiResponse.success(result, "Product suggestions retrieved successfully"));
    }

    /**
     * GET /api/product-suggestions/product-types
     * Distinct product-type codes present in the suggestion list.
     */
    @GetMapping("/product-types")
    public ResponseEntity<ApiResponse<List<String>>> getProductTypes() {
        log.info("Request: Product suggestion product-type codes");
        List<String> codes = productSuggestionService.getProductTypeCodes();
        return ResponseEntity.ok(ApiResponse.success(codes, "Product type codes retrieved successfully"));
    }

    /**
     * POST /api/product-suggestions
     * Create a new suggestion.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProductSuggestionDTO>> create(
            @Valid @RequestBody ProductSuggestionRequest request) {
        log.info("Request: Create product suggestion name='{}'", request.getName());
        ProductSuggestionDTO created = productSuggestionService.create(request);
        return ResponseEntity.ok(ApiResponse.success(created, "Product suggestion created successfully"));
    }

    /**
     * PUT /api/product-suggestions/{id}
     * Update an existing suggestion.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductSuggestionDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductSuggestionRequest request) {
        log.info("Request: Update product suggestion id={}", id);
        ProductSuggestionDTO updated = productSuggestionService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Product suggestion updated successfully"));
    }

    /**
     * DELETE /api/product-suggestions/{id}
     * Hard-delete a suggestion.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        log.info("Request: Delete product suggestion id={}", id);
        productSuggestionService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Product suggestion deleted successfully"));
    }
}
