package com.knp.controller.tenant;

import com.knp.model.dto.ApiResponse;
import com.knp.model.dto.goldprice.GoldPriceDTO;
import com.knp.model.dto.goldprice.PriceBoardResponse;
import com.knp.service.goldprice.GoldPriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.knp.annotation.RequiresFeature;

@RestController
@RequestMapping("/gold-prices")
@RequiredArgsConstructor
@Slf4j
@RequiresFeature("PAWN")
public class GoldPriceController {

    private final GoldPriceService goldPriceService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<GoldPriceDTO>>> getAllPrices() {
        log.info("Endpoint: GET /gold-prices");
        return ResponseEntity.ok(ApiResponse.success(goldPriceService.getAllPrices(), "OK"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GoldPriceDTO>> updatePrice(
            @PathVariable Long id,
            @RequestBody GoldPriceDTO dto) {
        log.info("Endpoint: PUT /gold-prices/{}", id);
        return ResponseEntity.ok(ApiResponse.success(goldPriceService.updatePrice(id, dto), "Updated"));
    }

    /** Public endpoint — no JWT required. TV display calls this with X-Tenant-ID + optional code param. */
    @GetMapping("/price-board")
    public ResponseEntity<ApiResponse<PriceBoardResponse>> getPriceBoard(
            @RequestParam(required = false) String code) {
        log.info("Endpoint: GET /gold-prices/price-board");
        return ResponseEntity.ok(ApiResponse.success(goldPriceService.getPriceBoard(code), "OK"));
    }
}
