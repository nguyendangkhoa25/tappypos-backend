package com.knp.controller;

import com.knp.model.dto.ApiResponse;
import com.knp.model.dto.buyback.BuybackOrderDTO;
import com.knp.model.dto.buyback.CreateBuybackOrderRequest;
import com.knp.service.BuybackOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/buyback-orders")
@RequiredArgsConstructor
public class BuybackOrderController {

    private final BuybackOrderService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<BuybackOrderDTO>>> getAll(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("GET /api/buyback-orders type={} status={}", type, status);
        return ResponseEntity.ok(ApiResponse.success(service.getAll(type, status, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<BuybackOrderDTO>> getById(@PathVariable Long id) {
        log.info("GET /api/buyback-orders/{}", id);
        return ResponseEntity.ok(ApiResponse.success(service.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<BuybackOrderDTO>> create(@RequestBody @Valid CreateBuybackOrderRequest req) {
        log.info("POST /api/buyback-orders type={}", req.getType());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(service.create(req)));
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<BuybackOrderDTO>> complete(@PathVariable Long id) {
        log.info("PUT /api/buyback-orders/{}/complete", id);
        return ResponseEntity.ok(ApiResponse.success(service.complete(id)));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<BuybackOrderDTO>> cancel(@PathVariable Long id) {
        log.info("PUT /api/buyback-orders/{}/cancel", id);
        return ResponseEntity.ok(ApiResponse.success(service.cancel(id)));
    }
}
