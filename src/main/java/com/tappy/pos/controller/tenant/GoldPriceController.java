package com.tappy.pos.controller.tenant;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.goldprice.GoldPriceDTO;
import com.tappy.pos.model.dto.goldprice.PriceBoardResponse;
import com.tappy.pos.service.goldprice.GoldPriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/gold-prices")
@RequiredArgsConstructor
@Slf4j
public class GoldPriceController {

    private final GoldPriceService goldPriceService;

    @GetMapping
    @RequiresFeature("GOLD_PRICE")
    public ResponseEntity<ApiResponse<List<GoldPriceDTO>>> getAllPrices() {
        log.info("Endpoint: GET /gold-prices");
        return ResponseEntity.ok(ApiResponse.success(goldPriceService.getAllPrices(), "OK"));
    }

    @GetMapping("/current")
    @RequiresFeature("GOLD_PRICE")
    public ResponseEntity<ApiResponse<GoldPriceDTO>> getCurrentPrice() {
        log.info("Endpoint: GET /gold-prices/current");
        List<GoldPriceDTO> all = goldPriceService.getAllPrices();
        GoldPriceDTO current = all.isEmpty() ? null : all.get(0);
        return ResponseEntity.ok(ApiResponse.success(current, "OK"));
    }

    @GetMapping("/history")
    @RequiresFeature("GOLD_PRICE")
    public ResponseEntity<ApiResponse<List<GoldPriceDTO>>> getHistory(
            @RequestParam(defaultValue = "30") int days) {
        log.info("Endpoint: GET /gold-prices/history days={}", days);
        return ResponseEntity.ok(ApiResponse.success(goldPriceService.getAllPrices(), "OK"));
    }

    @PostMapping
    @RequiresFeature("GOLD_PRICE")
    public ResponseEntity<ApiResponse<GoldPriceDTO>> createPrice(@RequestBody GoldPriceDTO dto) {
        log.info("Endpoint: POST /gold-prices");
        return ResponseEntity.ok(ApiResponse.success(goldPriceService.createPrice(dto), "Created"));
    }

    @PutMapping("/{id}")
    @RequiresFeature("GOLD_PRICE")
    public ResponseEntity<ApiResponse<GoldPriceDTO>> updatePrice(
            @PathVariable Long id,
            @RequestBody GoldPriceDTO dto) {
        log.info("Endpoint: PUT /gold-prices/{}", id);
        return ResponseEntity.ok(ApiResponse.success(goldPriceService.updatePrice(id, dto), "Updated"));
    }

    @DeleteMapping("/{id}")
    @RequiresFeature("GOLD_PRICE")
    public ResponseEntity<ApiResponse<Void>> deletePrice(@PathVariable Long id) {
        log.info("Endpoint: DELETE /gold-prices/{}", id);
        goldPriceService.deletePrice(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Deleted"));
    }

    /** Used by PawnForm and future jewelry/order forms to auto-suggest prices when a category is selected. */
    @GetMapping("/for-category/{categoryId}")
    @RequiresFeature({"GOLD_PRICE", "PAWN"})
    public ResponseEntity<ApiResponse<GoldPriceDTO>> getPriceForCategory(@PathVariable Long categoryId) {
        log.info("Endpoint: GET /gold-prices/for-category/{}", categoryId);
        return ResponseEntity.ok(ApiResponse.success(goldPriceService.getPriceForCategory(categoryId), "OK"));
    }

    /** Public endpoint — no JWT required. TV display calls this with X-Tenant-ID + optional code param. */
    @GetMapping("/price-board")
    public ResponseEntity<ApiResponse<PriceBoardResponse>> getPriceBoard(
            @RequestParam(required = false) String code) {
        log.info("Endpoint: GET /gold-prices/price-board");
        return ResponseEntity.ok(ApiResponse.success(goldPriceService.getPriceBoard(code), "OK"));
    }
}
