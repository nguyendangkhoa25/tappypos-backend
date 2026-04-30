package com.knp.controller.customer;

import com.knp.model.dto.ApiResponse;
import com.knp.model.dto.loyalty.*;
import com.knp.service.customer.LoyaltyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import com.knp.annotation.RequiresFeature;

@Slf4j
@RestController
@RequestMapping("/loyalty")
@RequiredArgsConstructor
@RequiresFeature("LOYALTY")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    // ── Program ───────────────────────────────────────────────────────────────

    @GetMapping("/program")
    public ResponseEntity<ApiResponse<LoyaltyProgramDTO>> getProgram() {
        log.info("Endpoint: GET /loyalty/program");
        return ResponseEntity.ok(ApiResponse.success(loyaltyService.getProgram()));
    }

    @PutMapping("/program")
    public ResponseEntity<ApiResponse<LoyaltyProgramDTO>> saveProgram(@RequestBody SaveLoyaltyProgramRequest req) {
        log.info("Endpoint: PUT /loyalty/program");
        return ResponseEntity.ok(ApiResponse.success(loyaltyService.saveProgram(req), "Program updated successfully"));
    }

    // ── Tiers ─────────────────────────────────────────────────────────────────

    @GetMapping("/tiers")
    public ResponseEntity<ApiResponse<List<LoyaltyTierDTO>>> getTiers() {
        log.info("Endpoint: GET /loyalty/tiers");
        return ResponseEntity.ok(ApiResponse.success(loyaltyService.getTiers()));
    }

    @PostMapping("/tiers")
    public ResponseEntity<ApiResponse<LoyaltyTierDTO>> createTier(@RequestBody CreateLoyaltyTierRequest req) {
        log.info("Endpoint: POST /loyalty/tiers - name: {}", req.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(loyaltyService.createTier(req), "Tier created successfully"));
    }

    @PutMapping("/tiers/{id}")
    public ResponseEntity<ApiResponse<LoyaltyTierDTO>> updateTier(
            @PathVariable Long id,
            @RequestBody CreateLoyaltyTierRequest req) {
        log.info("Endpoint: PUT /loyalty/tiers/{}", id);
        return ResponseEntity.ok(ApiResponse.success(loyaltyService.updateTier(id, req), "Tier updated successfully"));
    }

    @DeleteMapping("/tiers/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTier(@PathVariable Long id) {
        log.info("Endpoint: DELETE /loyalty/tiers/{}", id);
        loyaltyService.deleteTier(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Tier deleted successfully"));
    }

    // ── Customer Loyalty ──────────────────────────────────────────────────────

    @GetMapping("/customers/{customerId}/summary")
    public ResponseEntity<ApiResponse<CustomerLoyaltySummaryDTO>> getCustomerLoyalty(@PathVariable Long customerId) {
        log.info("Endpoint: GET /loyalty/customers/{}/summary", customerId);
        return ResponseEntity.ok(ApiResponse.success(loyaltyService.getCustomerLoyalty(customerId)));
    }

    @GetMapping("/customers/{customerId}/transactions")
    public ResponseEntity<ApiResponse<Page<LoyaltyTransactionDTO>>> getTransactions(
            @PathVariable Long customerId, Pageable pageable) {
        log.info("Endpoint: GET /loyalty/customers/{}/transactions", customerId);
        return ResponseEntity.ok(ApiResponse.success(loyaltyService.getTransactionHistory(customerId, pageable)));
    }

    @PostMapping("/customers/{customerId}/adjust")
    public ResponseEntity<ApiResponse<LoyaltyTransactionDTO>> adjustPoints(
            @PathVariable Long customerId,
            @RequestBody Map<String, Object> body) {
        int points = (Integer) body.get("points");
        String description = (String) body.getOrDefault("description", null);
        log.info("Endpoint: POST /loyalty/customers/{}/adjust - points: {}", customerId, points);
        return ResponseEntity.ok(ApiResponse.success(loyaltyService.adjustPoints(customerId, points, description)));
    }
}
