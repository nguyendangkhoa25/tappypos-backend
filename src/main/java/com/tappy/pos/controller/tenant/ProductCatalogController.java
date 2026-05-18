package com.tappy.pos.controller.tenant;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.product.ProductCatalogDTO;
import com.tappy.pos.model.dto.product.ProductCatalogSyncResult;
import com.tappy.pos.service.product.ProductCatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Master-admin endpoints for managing the shared product catalog.
 * Requires PRODUCT_CATALOG feature (MASTER_TENANT only; AGENT excluded).
 */
@RestController
@RequestMapping("/product-catalog")
@RequiredArgsConstructor
@Slf4j
@RequiresFeature("PRODUCT_CATALOG")
public class ProductCatalogController {

    private final ProductCatalogService productCatalogService;

    /**
     * GET /api/product-catalog?search=&page=0&size=20
     * Search / list catalog entries (paginated).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductCatalogDTO>>> search(
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Request: Product catalog search='{}' page={} size={}", search, page, size);
        Page<ProductCatalogDTO> result = productCatalogService.search(search, page, size);
        return ResponseEntity.ok(ApiResponse.success(result, "Product catalog retrieved successfully"));
    }

    /**
     * GET /api/product-catalog/stats
     * Returns total, fromOff, and manual counts.
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStats() {
        log.info("Request: Product catalog stats");
        long total = productCatalogService.getTotalCount();
        long fromOff = productCatalogService.countBySource("OPEN_FOOD_FACTS");
        long manual = total - fromOff;
        Map<String, Long> stats = Map.of(
                "total", total,
                "fromOff", fromOff,
                "manual", manual
        );
        return ResponseEntity.ok(ApiResponse.success(stats, "Stats retrieved successfully"));
    }

    /**
     * POST /api/product-catalog/sync?maxPages=10
     * Trigger a sync from Open Food Facts.
     */
    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<ProductCatalogSyncResult>> sync(
            @RequestParam(defaultValue = "10") int maxPages) {
        log.info("Request: Sync product catalog from Open Food Facts, maxPages={}", maxPages);
        ProductCatalogSyncResult result = productCatalogService.syncFromOpenFoodFacts(maxPages);
        log.info("Sync complete: inserted={}, updated={}, skipped={}, totalFetched={}, pagesProcessed={}",
                result.getInserted(), result.getUpdated(), result.getSkipped(),
                result.getTotalFetched(), result.getPagesProcessed());
        return ResponseEntity.ok(ApiResponse.success(result, "Sync completed successfully"));
    }
}
