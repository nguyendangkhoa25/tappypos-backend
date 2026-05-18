package com.tappy.pos.controller.vendor;

import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.vendor.CreatePurchaseOrderRequest;
import com.tappy.pos.model.dto.vendor.PurchaseOrderDTO;
import com.tappy.pos.model.dto.vendor.ReceiveItemsRequest;
import com.tappy.pos.service.vendor.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.tappy.pos.annotation.RequiresFeature;

@Slf4j
@RestController
@RequestMapping("/purchase-orders")
@RequiredArgsConstructor
@RequiresFeature("VENDOR")
public class PurchaseOrderController {

    private final PurchaseOrderService poService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PurchaseOrderDTO>>> getAll(
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(poService.getAll(status, pageable), "Purchase orders retrieved"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseOrderDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(poService.getById(id), "Purchase order retrieved"));
    }

    @GetMapping("/by-vendor/{vendorId}")
    public ResponseEntity<ApiResponse<Page<PurchaseOrderDTO>>> getByVendor(
            @PathVariable Long vendorId, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(poService.getByVendor(vendorId, pageable), "Purchase orders retrieved"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseOrderDTO>> create(@Valid @RequestBody CreatePurchaseOrderRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(poService.create(req), "Purchase order created"));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<PurchaseOrderDTO>> submit(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(poService.submit(id), "Purchase order submitted"));
    }

    @PostMapping("/{id}/receive")
    public ResponseEntity<ApiResponse<PurchaseOrderDTO>> receive(@PathVariable Long id,
                                                                  @RequestBody ReceiveItemsRequest req) {
        return ResponseEntity.ok(ApiResponse.success(poService.receiveItems(id, req), "Items received"));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<PurchaseOrderDTO>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(poService.cancel(id), "Purchase order cancelled"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        poService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Purchase order deleted"));
    }
}
