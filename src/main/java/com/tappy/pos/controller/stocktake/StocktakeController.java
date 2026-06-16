package com.tappy.pos.controller.stocktake;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.stocktake.*;
import com.tappy.pos.model.enums.StocktakeStatus;
import com.tappy.pos.service.stocktake.StocktakeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Stocktake / inventory verification ("Kiểm Kho").
 * Session-based physical count: scan/enter counted quantities, review discrepancies, apply.
 */
@Slf4j
@RestController
@RequestMapping("/stocktake")
@RequiredArgsConstructor
@RequiresFeature("STOCK_TAKE")
public class StocktakeController {

    private final StocktakeService stocktakeService;

    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<StocktakeSessionDTO>> createSession(
            @RequestBody(required = false) CreateStocktakeSessionRequest request) {
        log.info("POST /stocktake/sessions - start session");
        StocktakeSessionDTO session = stocktakeService.createSession(
                request != null ? request : new CreateStocktakeSessionRequest());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(session, "Stocktake session started"));
    }

    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<Page<StocktakeSessionDTO>>> listSessions(
            @RequestParam(required = false) StocktakeStatus status, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                stocktakeService.listSessions(status, pageable), "Stocktake sessions retrieved"));
    }

    @GetMapping("/sessions/active")
    public ResponseEntity<ApiResponse<StocktakeSessionDTO>> getActiveSession() {
        return ResponseEntity.ok(ApiResponse.success(
                stocktakeService.getActiveSession(), "Active stocktake session retrieved"));
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<ApiResponse<StocktakeSessionDTO>> getSession(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                stocktakeService.getSession(id), "Stocktake session retrieved"));
    }

    @GetMapping("/sessions/{id}/lookup")
    public ResponseEntity<ApiResponse<StocktakeProductLineDTO>> lookup(
            @PathVariable Long id, @RequestParam String barcode) {
        return ResponseEntity.ok(ApiResponse.success(
                stocktakeService.lookup(id, barcode), "Product resolved"));
    }

    @PostMapping("/sessions/{id}/counts")
    public ResponseEntity<ApiResponse<StocktakeCountDTO>> upsertCount(
            @PathVariable Long id, @RequestBody @Valid UpsertCountRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                stocktakeService.upsertCount(id, request), "Count saved"));
    }

    @DeleteMapping("/sessions/{id}/counts/{countId}")
    public ResponseEntity<ApiResponse<Void>> deleteCount(
            @PathVariable Long id, @PathVariable Long countId) {
        stocktakeService.deleteCount(id, countId);
        return ResponseEntity.ok(ApiResponse.success(null, "Count removed"));
    }

    @GetMapping("/sessions/{id}/discrepancies")
    public ResponseEntity<ApiResponse<List<StocktakeCountDTO>>> getDiscrepancies(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                stocktakeService.getDiscrepancies(id), "Discrepancies retrieved"));
    }

    @GetMapping("/sessions/{id}/uncounted")
    public ResponseEntity<ApiResponse<List<StocktakeProductLineDTO>>> getUncounted(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                stocktakeService.getUncounted(id), "Uncounted products retrieved"));
    }

    @PostMapping("/sessions/{id}/apply")
    public ResponseEntity<ApiResponse<StocktakeSessionDTO>> apply(@PathVariable Long id) {
        log.info("POST /stocktake/sessions/{}/apply", id);
        return ResponseEntity.ok(ApiResponse.success(
                stocktakeService.apply(id), "Stocktake applied"));
    }

    @PostMapping("/sessions/{id}/cancel")
    public ResponseEntity<ApiResponse<StocktakeSessionDTO>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                stocktakeService.cancel(id), "Stocktake cancelled"));
    }
}
