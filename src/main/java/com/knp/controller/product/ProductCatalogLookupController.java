package com.knp.controller.product;

import com.knp.annotation.RequiresFeature;
import com.knp.model.dto.ApiResponse;
import com.knp.model.dto.product.ProductCatalogDTO;
import com.knp.service.product.ProductCatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * ProductCatalogLookupController - Tenant-facing barcode lookup endpoint.
 * Requires PRODUCT feature in the caller's JWT.
 */
@RestController
@RequestMapping("/product-catalog")
@RequiredArgsConstructor
@Slf4j
@RequiresFeature("PRODUCT")
public class ProductCatalogLookupController {

    private final ProductCatalogService productCatalogService;

    /**
     * GET /api/product-catalog/{barcode}
     * Look up a product by barcode. Returns 200 with data if found, 200 with null data and message if not found.
     */
    @GetMapping("/{barcode}")
    public ResponseEntity<ApiResponse<ProductCatalogDTO>> findByBarcode(@PathVariable String barcode) {
        log.info("Request: Product catalog lookup barcode={}", barcode);
        Optional<ProductCatalogDTO> result = productCatalogService.findByBarcode(barcode);
        if (result.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success(result.get(), "Product found"));
        }
        return ResponseEntity.ok(ApiResponse.<ProductCatalogDTO>builder()
                .success(true)
                .message("not found")
                .data(null)
                .build());
    }
}
