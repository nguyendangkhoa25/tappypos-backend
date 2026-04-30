package com.knp.controller.finance;

import com.knp.model.dto.ApiResponse;
import com.knp.model.dto.revenue.CategoryRevenueDTO;
import com.knp.model.dto.revenue.DayOfWeekRevenueDTO;
import com.knp.model.dto.revenue.HourlyRevenueDTO;
import com.knp.model.dto.revenue.PaymentBreakdownDTO;
import com.knp.model.dto.revenue.RevenueOverviewDTO;
import com.knp.model.dto.revenue.RevenuePeriodDTO;
import com.knp.model.dto.revenue.TopEmployeeDTO;
import com.knp.model.dto.revenue.TopProductDTO;
import com.knp.service.finance.RevenueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import com.knp.annotation.RequiresFeature;

@Slf4j
@RestController
@RequestMapping("/revenue")
@RequiredArgsConstructor
@RequiresFeature("REVENUE")
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

    @GetMapping("/day-of-week")
    public ResponseEntity<ApiResponse<List<DayOfWeekRevenueDTO>>> getDayOfWeekBreakdown(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        log.info("Endpoint: GET /revenue/day-of-week - year: {}, month: {}", year, month);
        return ResponseEntity.ok(ApiResponse.success(revenueService.getDayOfWeekBreakdown(year, month), "Day-of-week breakdown retrieved"));
    }

    @GetMapping("/hourly")
    public ResponseEntity<ApiResponse<List<HourlyRevenueDTO>>> getHourlyBreakdown(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        log.info("Endpoint: GET /revenue/hourly - year: {}, month: {}", year, month);
        return ResponseEntity.ok(ApiResponse.success(revenueService.getHourlyBreakdown(year, month), "Hourly breakdown retrieved"));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryRevenueDTO>>> getCategoryBreakdown(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        log.info("Endpoint: GET /revenue/categories - year: {}, month: {}", year, month);
        return ResponseEntity.ok(ApiResponse.success(revenueService.getCategoryBreakdown(year, month), "Category breakdown retrieved"));
    }

    @GetMapping("/top-employees")
    public ResponseEntity<ApiResponse<List<TopEmployeeDTO>>> getTopEmployees(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Endpoint: GET /revenue/top-employees - year: {}, month: {}, limit: {}", year, month, limit);
        return ResponseEntity.ok(ApiResponse.success(revenueService.getTopEmployees(year, month, limit), "Top employees retrieved"));
    }
}
