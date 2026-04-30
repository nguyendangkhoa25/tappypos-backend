package com.knp.controller.buyback;

import com.knp.model.dto.ApiResponse;
import com.knp.model.dto.buyback.BuybackOrderDTO;
import com.knp.model.dto.buyback.CreateBuybackOrderRequest;
import com.knp.service.buyback.BuybackOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.knp.annotation.RequiresFeature;

@Slf4j
@RestController
@RequestMapping("/buyback-orders")
@RequiredArgsConstructor
@RequiresFeature("PAWN")
public class BuybackOrderController {

    private final BuybackOrderService service;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<BuybackOrderDTO>>> getAll(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("GET /api/buyback-orders type={} status={}", type, status);
        return ResponseEntity.ok(ApiResponse.success(service.getAll(type, status, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BuybackOrderDTO>> getById(@PathVariable Long id) {
        log.info("GET /api/buyback-orders/{}", id);
        return ResponseEntity.ok(ApiResponse.success(service.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BuybackOrderDTO>> create(@RequestBody @Valid CreateBuybackOrderRequest req) {
        log.info("POST /api/buyback-orders type={}", req.getType());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(service.create(req)));
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<BuybackOrderDTO>> complete(@PathVariable Long id) {
        log.info("PUT /api/buyback-orders/{}/complete", id);
        return ResponseEntity.ok(ApiResponse.success(service.complete(id)));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BuybackOrderDTO>> cancel(@PathVariable Long id) {
        log.info("PUT /api/buyback-orders/{}/cancel", id);
        return ResponseEntity.ok(ApiResponse.success(service.cancel(id)));
    }
}
