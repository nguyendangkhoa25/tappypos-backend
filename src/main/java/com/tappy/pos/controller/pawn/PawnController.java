package com.tappy.pos.controller.pawn;

import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.goldprice.GoldPriceDTO;
import com.tappy.pos.model.dto.pawn.*;
import com.tappy.pos.service.goldprice.GoldPriceService;
import com.tappy.pos.service.pawn.PawnService;
import com.tappy.pos.exception.ForbiddenException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import com.tappy.pos.annotation.RequiresFeature;

/**
 * PawnController - REST API endpoints for pawn management
 */
@RestController
@RequestMapping("/pawns")
@RequiredArgsConstructor
@Slf4j
@RequiresFeature("PAWN")
public class PawnController {
    private final PawnService pawnService;
    private final GoldPriceService goldPriceService;

    /**
     * Guards an endpoint so only SHOP_OWNER may proceed.
     * Throws ForbiddenException (→ HTTP 403) for any other role.
     * Mirrors the pattern used in ShopDeletionService.
     */
    private void requireShopOwner() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isShopOwner = auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("SHOP_OWNER"::equals);
        if (!isShopOwner) {
            throw new ForbiddenException("Chỉ chủ cửa hàng mới có quyền thực hiện thao tác này.");
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PawnResponse>> createPawn(@Valid @RequestBody PawnRequest pawnRequest) {
        log.info("Request: Create pawn");
        log.debug("Create pawn request body: {}", pawnRequest);
        PawnResponse response = pawnService.createPawn(pawnRequest);
        log.info("Pawn created successfully");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Pawn created successfully"));
    }

    @PostMapping("/find")
    public ResponseEntity<ApiResponse<PawnSearchResponse>> getPawns(Pageable pageable, @Valid @RequestBody SearchPawnRequest searchRequest) {
        log.info("Request: Search pawns with criteria");
        PawnSearchResponse response = pawnService.getPawns(pageable, searchRequest);
        log.info("Found {} pawns", response.getTotalElements());
        return ResponseEntity.ok(
                ApiResponse.success(response, "Pawns retrieved successfully")
        );
    }

    @PutMapping("/{pawnId}")
    public ResponseEntity<ApiResponse<PawnResponse>> updatePawn(@PathVariable Long pawnId, @Valid @RequestBody PawnRequest pawnRequest) {
        log.info("Request: Update pawn: {}", pawnId);
        log.debug("Update pawn [{}] request body: {}", pawnId, pawnRequest);
        PawnResponse response = pawnService.updatePawn(pawnId, pawnRequest);
        log.info("Pawn updated: {}", pawnId);
        return ResponseEntity.ok(
                ApiResponse.success(response, "Pawn updated successfully")
        );
    }

    @GetMapping("/{pawnId}")
    public ResponseEntity<ApiResponse<PawnResponse>> getPawnDetails(@PathVariable Long pawnId) {
        log.info("Request: Get pawn details: {}", pawnId);
        PawnResponse response = pawnService.getPawnDetails(pawnId);
        log.info("Retrieved pawn: {}", pawnId);
        return ResponseEntity.ok(
                ApiResponse.success(response, "Pawn details retrieved successfully")
        );
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deletePawnsByPawnIds(@RequestBody List<Long> pawnIds) {
        requireShopOwner();
        log.info("Request: Delete {} pawns", pawnIds.size());
        pawnService.deletePawnByPawnIds(pawnIds);
        log.info("Pawns deleted successfully");
        return ResponseEntity.ok(
                ApiResponse.success(null, "Pawns deleted successfully")
        );
    }

    @PatchMapping("/{pawnId}/cancel")
    public ResponseEntity<ApiResponse<PawnResponse>> cancelPawnByPawnId(@PathVariable Long pawnId, @RequestBody String cancelReason) {
        log.info("Request: Cancel pawn: {}", pawnId);
        PawnResponse response = pawnService.cancelPawnByPawnId(pawnId, cancelReason);
        log.info("Pawn cancelled: {}", pawnId);
        return ResponseEntity.ok(
                ApiResponse.success(response, "Pawn cancelled successfully")
        );
    }

    @PostMapping("/{pawnId}/forfeit")
    public ResponseEntity<ApiResponse<PawnResponse>> forfeitPawnByPawnId(@PathVariable Long pawnId, @Valid @RequestBody ForfeitRequest forfeitRequest) {
        log.info("Request: Forfeit pawn: {}", pawnId);
        PawnResponse response = pawnService.forfeitPawnByPawnId(pawnId, forfeitRequest);
        log.info("Pawn forfeited: {}", pawnId);
        return ResponseEntity.ok(
                ApiResponse.success(response, "Pawn forfeited successfully")
        );
    }

    @PostMapping("/{pawnId}/redeem")
    public ResponseEntity<ApiResponse<PawnResponse>> calculatePawnRedeem(@PathVariable Long pawnId, @Valid @RequestBody RedeemRequest redeemRequest) {
        log.info("Request: Redeem pawn: {}", pawnId);
        PawnResponse response = pawnService.calculatePawnRedeem(pawnId, redeemRequest);
        log.info("Pawn redeemed: {}", pawnId);
        return ResponseEntity.ok(
                ApiResponse.success(response, "Pawn redeemed successfully")
        );
    }

    @PostMapping("/{pawnId}/request-money")
    public ResponseEntity<ApiResponse<ReqMoneyResponse>> requestMoreMoney(@PathVariable Long pawnId, @Valid @RequestBody ReqMoneyRequest reqMoneyRequest) {
        log.info("Request: Request more money for pawn: {}", pawnId);
        ReqMoneyResponse response = pawnService.requestMoreMoney(pawnId, reqMoneyRequest);
        log.info("Request created: {}", pawnId);
        return ResponseEntity.ok(
                ApiResponse.success(response, "Money request created successfully")
        );
    }

    @PutMapping("/{pawnId}/extend")
    public ResponseEntity<ApiResponse<PawnResponse>> extendPawn(@PathVariable Long pawnId, @Valid @RequestBody PawnRequest pawnRequest) {
        log.info("Request: Extend pawn: {}", pawnId);
        PawnResponse response = pawnService.extendPawn(pawnId, pawnRequest);
        log.info("Pawn extended: {}", pawnId);
        return ResponseEntity.ok(
                ApiResponse.success(response, "Pawn extended successfully")
        );
    }

    @PostMapping("/kpi-section")
    public ResponseEntity<ApiResponse<PawnKPIs>> getPawnKPIs(@Valid @RequestBody DateFilterRequest dateFilter) {
        log.info("Request: Get pawn KPIs");
        PawnKPIs response = pawnService.getPawnKPIs(dateFilter);
        log.info("Pawn KPIs retrieved");
        return ResponseEntity.ok(
                ApiResponse.success(response, "Pawn KPIs retrieved successfully")
        );
    }

    @PostMapping("/customer-kpi")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCustomerPawnKpi(
            @Valid @RequestBody DateFilterRequest dateFilter) {
        log.info("Request: Get customer pawn KPI rankings");
        return ResponseEntity.ok(ApiResponse.success(
                pawnService.getCustomerPawnKpi(dateFilter), "Customer pawn KPIs retrieved successfully"));
    }

    @PostMapping("/export")
    public ResponseEntity<FileSystemResource> exportPawns(@Valid @RequestBody SearchPawnRequest searchRequest) throws IOException {
        log.info("Request: Export pawns");
        FileSystemResource fileSystemResource = pawnService.exportPawns(searchRequest);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"danhsach-camdo.xlsx\"");
        log.info("Pawns exported successfully");
        return new ResponseEntity<>(fileSystemResource, headers, HttpStatus.OK);
    }

    @PostMapping("/charts")
    public ResponseEntity<ApiResponse<PawnBarsResponse>> getPawnBars(@Valid @RequestBody DateFilterRequest dateFilter) {
        log.info("Request: Get pawn charts");
        PawnBarsResponse response = pawnService.getPawnCharts(dateFilter);
        log.info("Pawn charts retrieved");
        return ResponseEntity.ok(
                ApiResponse.success(response, "Pawn charts retrieved successfully")
        );
    }

    @PatchMapping("/{pawnId}/visible")
    public ResponseEntity<ApiResponse<Integer>> updateVisibleStatus(@PathVariable Long pawnId, @Valid @RequestBody PawnRequest pawnRequest) {
        requireShopOwner();
        log.info("Request: Update visible status for pawn: {}", pawnId);
        int result = pawnService.updateVisibleStatus(pawnId, pawnRequest.isVisible());
        log.info("Visible status updated: {}", pawnId);
        return ResponseEntity.ok(
                ApiResponse.success(result, "Visible status updated successfully")
        );
    }

    @PostMapping("/settings")
    public ResponseEntity<ApiResponse<PawnSetting>> updatePawnSetting(@Valid @RequestBody PawnSetting settingReq) {
        requireShopOwner();
        log.info("Request: Update pawn settings");
        PawnSetting response = pawnService.updatePawnSetting(settingReq);
        log.info("Pawn settings updated");
        return ResponseEntity.ok(
                ApiResponse.success(response, "Pawn settings updated successfully")
        );
    }

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<PawnSetting>> getPawnSetting() {
        log.info("Request: Get pawn settings");
        PawnSetting response = pawnService.getPawnSetting();
        log.info("Pawn settings retrieved");
        return ResponseEntity.ok(
                ApiResponse.success(response, "Pawn settings retrieved successfully")
        );
    }

    @GetMapping("/lookup")
    public ResponseEntity<ApiResponse<PawnResponse>> lookupPawn(@RequestParam String q) {
        log.info("Request: Lookup pawn by code: {}", q);
        PawnResponse response = pawnService.lookupByCode(q);
        log.info("Pawn lookup found: {}", response.getPawnId());
        return ResponseEntity.ok(ApiResponse.success(response, "Pawn found"));
    }

    @GetMapping("/customer-summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCustomerPawnSummary(
            @RequestParam Long customerId) {
        log.info("GET /pawns/customer-summary?customerId={}", customerId);
        return ResponseEntity.ok(ApiResponse.success(pawnService.getCustomerPawnSummary(customerId), "OK"));
    }

    @GetMapping("/top-customers")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTopPawnCustomers(
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        log.info("Request: Get top pawn customers limit={} from={} to={}", limit, from, to);
        List<Map<String, Object>> response = pawnService.getTopPawnCustomers(limit, from, to);
        log.info("Top pawn customers retrieved: {} records", response.size());
        return ResponseEntity.ok(ApiResponse.success(response, "Top pawn customers retrieved successfully"));
    }

    @GetMapping("/customer-insights")
    public ResponseEntity<ApiResponse<PawnCustomerInsights>> getPawnCustomerInsights(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        log.info("Request: GET /pawns/customer-insights from={} to={}", from, to);
        return ResponseEntity.ok(ApiResponse.success(pawnService.getPawnCustomerInsights(from, to), "OK"));
    }

    /** Returns shop gold prices so the JEWELRY pawn form can compute item value without requiring the GOLD_PRICE feature. */
    @GetMapping("/gold-prices")
    public ResponseEntity<ApiResponse<List<GoldPriceDTO>>> getGoldPricesForPawn() {
        log.info("Request: GET /pawns/gold-prices");
        return ResponseEntity.ok(ApiResponse.success(goldPriceService.getAllPrices(), "OK"));
    }
}
