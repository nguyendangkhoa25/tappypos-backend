package com.tappy.pos.controller.tradein;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.tradein.CreateTradeInRequest;
import com.tappy.pos.model.dto.tradein.TradeInDTO;
import com.tappy.pos.model.enums.TradeInStatus;
import com.tappy.pos.service.tradein.TradeInService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Trade-in (thu cũ đổi mới / mua xe cũ). Gated by the TRADE_IN feature; the service further scopes
 * list/detail by TRADE_IN_VIEW_ALL. See VEHICLE_SHOP_SHOP_TYPE_PLAN §4c.
 */
@RestController
@RequestMapping("/trade-ins")
@RequiredArgsConstructor
@Slf4j
@RequiresFeature("TRADE_IN")
public class TradeInController {

    private final TradeInService tradeInService;

    @PostMapping
    public ResponseEntity<ApiResponse<TradeInDTO>> create(@Valid @RequestBody CreateTradeInRequest request) {
        log.info("Request: Create trade-in");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(tradeInService.create(request), "Trade-in created"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TradeInDTO>>> search(
            @RequestParam(required = false) TradeInStatus status, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(tradeInService.search(status, pageable), "OK"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TradeInDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(tradeInService.getById(id), "OK"));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<TradeInDTO>> cancel(
            @PathVariable Long id, @RequestBody(required = false) String reason) {
        return ResponseEntity.ok(ApiResponse.success(tradeInService.cancel(id, reason), "Trade-in cancelled"));
    }
}
