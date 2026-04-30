package com.knp.controller;

import com.knp.model.dto.ApiResponse;
import com.knp.model.dto.promotion.ApplyPromotionResponse;
import com.knp.model.dto.promotion.PromotionDTO;
import com.knp.model.dto.promotion.SavePromotionRequest;
import com.knp.service.order.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import com.knp.annotation.RequiresFeature;

@Slf4j
@RestController
@RequestMapping("/promotions")
@RequiredArgsConstructor
@RequiresFeature("PROMOTION")
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PromotionDTO>>> getAll(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(promotionService.getAll(pageable), "Promotions retrieved"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PromotionDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(promotionService.getById(id), "Promotion retrieved"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PromotionDTO>> create(@Valid @RequestBody SavePromotionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(promotionService.create(req), "Promotion created"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PromotionDTO>> update(@PathVariable Long id,
                                                             @Valid @RequestBody SavePromotionRequest req) {
        return ResponseEntity.ok(ApiResponse.success(promotionService.update(id, req), "Promotion updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        promotionService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Promotion deleted"));
    }

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<ApplyPromotionResponse>> validate(
            @RequestParam String code,
            @RequestParam BigDecimal subtotal) {
        return ResponseEntity.ok(ApiResponse.success(
                promotionService.validatePromotion(code, subtotal), "Promotion valid"));
    }
}
