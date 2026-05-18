package com.tappy.pos.controller.finance;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.marketprice.MarketPriceDTO;
import com.tappy.pos.model.dto.marketprice.SaveMarketPriceRequest;
import com.tappy.pos.service.finance.MarketPriceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/market-prices")
@RequiredArgsConstructor
@RequiresFeature("PAWN")
public class MarketPriceController {

    private final MarketPriceService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MarketPriceDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(service.getAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MarketPriceDTO>> create(@RequestBody @Valid SaveMarketPriceRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(service.create(req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MarketPriceDTO>> update(
            @PathVariable Long id, @RequestBody @Valid SaveMarketPriceRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.update(id, req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
