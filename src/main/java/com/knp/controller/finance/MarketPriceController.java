package com.knp.controller.finance;

import com.knp.model.dto.ApiResponse;
import com.knp.model.dto.marketprice.MarketPriceDTO;
import com.knp.model.dto.marketprice.SaveMarketPriceRequest;
import com.knp.service.finance.MarketPriceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.knp.annotation.RequiresFeature;

@Slf4j
@RestController
@RequestMapping("/market-prices")
@RequiredArgsConstructor
@RequiresFeature("PAWN")
public class MarketPriceController {

    private final MarketPriceService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MarketPriceDTO>>> getAll() {
        log.info("GET /api/market-prices");
        return ResponseEntity.ok(ApiResponse.success(service.getAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MarketPriceDTO>> create(@RequestBody @Valid SaveMarketPriceRequest req) {
        log.info("POST /api/market-prices - {}", req.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(service.create(req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MarketPriceDTO>> update(
            @PathVariable Long id, @RequestBody @Valid SaveMarketPriceRequest req) {
        log.info("PUT /api/market-prices/{}", id);
        return ResponseEntity.ok(ApiResponse.success(service.update(id, req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        log.info("DELETE /api/market-prices/{}", id);
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
