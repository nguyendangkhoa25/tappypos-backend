package com.tappy.pos.controller.pawn;

import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.pawn.*;
import com.tappy.pos.service.pawn.PawnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
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

    @PostMapping
    public ResponseEntity<ApiResponse<PawnResponse>> createPawn(@Valid @RequestBody PawnRequest pawnRequest) {
        log.info("Request: Create pawn");
        PawnResponse response = pawnService.createPawn(pawnRequest);
        log.info("Pawn created successfully");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Pawn created successfully"));
    }

    @PostMapping("/find")
    public ResponseEntity<ApiResponse<PawnSearchResponse>> getPawns(Pageable pageable, @RequestBody SearchPawnRequest searchRequest) {
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
    public ResponseEntity<ApiResponse<PawnResponse>> forfeitPawnByPawnId(@PathVariable Long pawnId, @RequestBody ForfeitRequest forfeitRequest) {
        log.info("Request: Forfeit pawn: {}", pawnId);
        PawnResponse response = pawnService.forfeitPawnByPawnId(pawnId, forfeitRequest);
        log.info("Pawn forfeited: {}", pawnId);
        return ResponseEntity.ok(
                ApiResponse.success(response, "Pawn forfeited successfully")
        );
    }

    @PostMapping("/{pawnId}/redeem")
    public ResponseEntity<ApiResponse<PawnResponse>> calculatePawnRedeem(@PathVariable Long pawnId, @RequestBody RedeemRequest redeemRequest) {
        log.info("Request: Redeem pawn: {}", pawnId);
        PawnResponse response = pawnService.calculatePawnRedeem(pawnId, redeemRequest);
        log.info("Pawn redeemed: {}", pawnId);
        return ResponseEntity.ok(
                ApiResponse.success(response, "Pawn redeemed successfully")
        );
    }

    @PostMapping("/{pawnId}/request-money")
    public ResponseEntity<ApiResponse<ReqMoneyResponse>> requestMoreMoney(@PathVariable Long pawnId, @RequestBody ReqMoneyRequest reqMoneyRequest) {
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
    public ResponseEntity<ApiResponse<PawnKPIs>> getPawnKPIs(@RequestBody DateFilterRequest dateFilter) {
        log.info("Request: Get pawn KPIs");
        PawnKPIs response = pawnService.getPawnKPIs(dateFilter);
        log.info("Pawn KPIs retrieved");
        return ResponseEntity.ok(
                ApiResponse.success(response, "Pawn KPIs retrieved successfully")
        );
    }

    @PostMapping("/export")
    public ResponseEntity<FileSystemResource> exportPawns(@RequestBody SearchPawnRequest searchRequest) throws IOException {
        log.info("Request: Export pawns");
        FileSystemResource fileSystemResource = pawnService.exportPawns(searchRequest);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"danhsach-camdo.xlsx\"");
        log.info("Pawns exported successfully");
        return new ResponseEntity<>(fileSystemResource, headers, HttpStatus.OK);
    }

    @PostMapping("/charts")
    public ResponseEntity<ApiResponse<PawnBarsResponse>> getPawnBars(@RequestBody DateFilterRequest dateFilter) {
        log.info("Request: Get pawn charts");
        PawnBarsResponse response = pawnService.getPawnCharts(dateFilter);
        log.info("Pawn charts retrieved");
        return ResponseEntity.ok(
                ApiResponse.success(response, "Pawn charts retrieved successfully")
        );
    }

    @PatchMapping("/{pawnId}/visible")
    public ResponseEntity<ApiResponse<Integer>> updateVisibleStatus(@PathVariable Long pawnId, @RequestBody PawnRequest pawnRequest) {
        log.info("Request: Update visible status for pawn: {}", pawnId);
        int result = pawnService.updateVisibleStatus(pawnId, pawnRequest.isVisible());
        log.info("Visible status updated: {}", pawnId);
        return ResponseEntity.ok(
                ApiResponse.success(result, "Visible status updated successfully")
        );
    }

    @PostMapping("/settings")
    public ResponseEntity<ApiResponse<PawnSetting>> updatePawnSetting(@RequestBody PawnSetting settingReq) {
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
}
