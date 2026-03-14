package com.knp.controller;

import com.knp.model.dto.ApiResponse;
import com.knp.model.dto.revenue.PaymentBreakdownDTO;
import com.knp.model.dto.revenue.RevenueOverviewDTO;
import com.knp.model.dto.revenue.RevenuePeriodDTO;
import com.knp.model.dto.revenue.TopProductDTO;
import com.knp.service.RevenueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/revenue")
@RequiredArgsConstructor
public class RevenueController {

    private final RevenueService revenueService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<RevenueOverviewDTO>> getOverview() {
        log.info("Endpoint: GET /revenue/overview");
        return ResponseEntity.ok(ApiResponse.success(revenueService.getOverview(), "Revenue overview retrieved"));
    }

    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<List<RevenuePeriodDTO>>> getMonthlyBreakdown(
            @RequestParam(defaultValue = "0") int year) {
        if (year == 0) year = LocalDate.now().getYear();
        log.info("Endpoint: GET /revenue/monthly - year: {}", year);
        return ResponseEntity.ok(ApiResponse.success(revenueService.getMonthlyBreakdown(year), "Monthly breakdown retrieved"));
    }

    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<List<RevenuePeriodDTO>>> getDailyBreakdown(
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "0") int month) {
        if (year  == 0) year  = LocalDate.now().getYear();
        if (month == 0) month = LocalDate.now().getMonthValue();
        log.info("Endpoint: GET /revenue/daily - year: {}, month: {}", year, month);
        return ResponseEntity.ok(ApiResponse.success(revenueService.getDailyBreakdown(year, month), "Daily breakdown retrieved"));
    }

    @GetMapping("/top-products")
    public ResponseEntity<ApiResponse<List<TopProductDTO>>> getTopProducts(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Endpoint: GET /revenue/top-products - year: {}, month: {}, limit: {}", year, month, limit);
        return ResponseEntity.ok(ApiResponse.success(revenueService.getTopProducts(year, month, limit), "Top products retrieved"));
    }

    @GetMapping("/payment-methods")
    public ResponseEntity<ApiResponse<List<PaymentBreakdownDTO>>> getPaymentBreakdown(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        log.info("Endpoint: GET /revenue/payment-methods - year: {}, month: {}", year, month);
        return ResponseEntity.ok(ApiResponse.success(revenueService.getPaymentBreakdown(year, month), "Payment breakdown retrieved"));
    }
}
