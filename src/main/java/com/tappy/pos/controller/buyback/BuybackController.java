package com.tappy.pos.controller.buyback;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.buyback.BuybackResponse;
import com.tappy.pos.model.dto.buyback.CreateBuybackRequest;
import com.tappy.pos.model.dto.buyback.SellBuybackRequest;
import com.tappy.pos.model.enums.BuybackStatus;
import com.tappy.pos.service.buyback.BuybackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Buyback / second-hand purchase ("mua bán đồ cũ"). Gated by the dedicated BUYBACK feature —
 * granted to the JEWELRY profile (and assignable to any used-goods shop, e.g. motorbike), NOT
 * to pawn shops (PAWN_BUYBACK_SPEC §7-Q3 / §8).
 */
@RestController
@RequestMapping("/buybacks")
@RequiredArgsConstructor
@Slf4j
@RequiresFeature("BUYBACK")
public class BuybackController {

    private final BuybackService buybackService;

    @PostMapping
    public ResponseEntity<ApiResponse<BuybackResponse>> createBuyback(@Valid @RequestBody CreateBuybackRequest request) {
        log.info("Request: Create buyback");
        BuybackResponse response = buybackService.createBuyback(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Buyback created successfully"));
    }

    @GetMapping("/{buybackId}")
    public ResponseEntity<ApiResponse<BuybackResponse>> getBuyback(@PathVariable Long buybackId) {
        log.info("Request: Get buyback: {}", buybackId);
        return ResponseEntity.ok(ApiResponse.success(buybackService.getBuyback(buybackId), "OK"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<BuybackResponse>>> getBuybacks(
            @RequestParam(required = false) BuybackStatus status, Pageable pageable) {
        log.info("Request: List buybacks status={}", status);
        return ResponseEntity.ok(ApiResponse.success(buybackService.getBuybacks(status, pageable), "OK"));
    }

    @PostMapping("/{buybackId}/sold")
    public ResponseEntity<ApiResponse<BuybackResponse>> markSold(
            @PathVariable Long buybackId, @Valid @RequestBody SellBuybackRequest request) {
        log.info("Request: Mark buyback sold: {}", buybackId);
        return ResponseEntity.ok(ApiResponse.success(buybackService.markSold(buybackId, request), "Buyback marked sold"));
    }

    @PatchMapping("/{buybackId}/cancel")
    public ResponseEntity<ApiResponse<BuybackResponse>> cancelBuyback(
            @PathVariable Long buybackId, @RequestBody(required = false) String reason) {
        log.info("Request: Cancel buyback: {}", buybackId);
        return ResponseEntity.ok(ApiResponse.success(buybackService.cancelBuyback(buybackId, reason), "Buyback cancelled"));
    }
}
