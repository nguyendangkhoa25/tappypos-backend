package com.tappy.pos.controller.product;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.product.GenerateVariantsRequest;
import com.tappy.pos.model.dto.product.ProductVariantDTO;
import com.tappy.pos.model.dto.product.SaveProductVariantRequest;
import com.tappy.pos.service.product.ProductVariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/products/{productId}/variants")
@RequiredArgsConstructor
@RequiresFeature("PRODUCT")
public class ProductVariantController {

    private final ProductVariantService productVariantService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductVariantDTO>>> getVariants(
            @PathVariable Long productId) {
        log.info("GET /products/{}/variants", productId);
        return ResponseEntity.ok(ApiResponse.success(productVariantService.getVariants(productId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductVariantDTO>> createVariant(
            @PathVariable Long productId,
            @RequestBody @Valid SaveProductVariantRequest req) {
        log.info("POST /products/{}/variants - sku={}", productId, req.getSku());
        ProductVariantDTO dto = productVariantService.createVariant(productId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(dto));
    }

    @PutMapping("/{variantId}")
    public ResponseEntity<ApiResponse<ProductVariantDTO>> updateVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @RequestBody @Valid SaveProductVariantRequest req) {
        log.info("PUT /products/{}/variants/{}", productId, variantId);
        return ResponseEntity.ok(ApiResponse.success(
                productVariantService.updateVariant(productId, variantId, req)));
    }

    @DeleteMapping("/{variantId}")
    public ResponseEntity<ApiResponse<Void>> deleteVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId) {
        log.info("DELETE /products/{}/variants/{}", productId, variantId);
        productVariantService.deleteVariant(productId, variantId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<List<ProductVariantDTO>>> generateVariants(
            @PathVariable Long productId,
            @RequestBody @Valid GenerateVariantsRequest req) {
        log.info("POST /products/{}/variants/generate", productId);
        List<ProductVariantDTO> generated = productVariantService.generateVariants(productId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(generated));
    }
}
