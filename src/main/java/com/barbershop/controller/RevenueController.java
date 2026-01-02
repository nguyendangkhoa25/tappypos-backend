package com.barbershop.controller;

import com.barbershop.model.dto.ApiResponse;
import com.barbershop.model.dto.revenue.CreateRevenueRequest;
import com.barbershop.model.dto.revenue.RevenueDTO;
import com.barbershop.service.RevenueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/revenues")
@RequiredArgsConstructor
public class RevenueController {

    private final RevenueService revenueService;

    /**
     * POST /api/revenues
     * Create a new revenue record for a specific month
     */
    @PostMapping
    public ResponseEntity<ApiResponse<RevenueDTO>> createRevenue(@RequestBody CreateRevenueRequest request) {
        log.info("Endpoint: POST /revenues - Create revenue for {}/{}", request.getMonth(), request.getYear());

        RevenueDTO revenue = revenueService.createRevenue(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(revenue, "Revenue created successfully"));
    }

    /**
     * GET /api/revenues/{id}
     * Get revenue by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RevenueDTO>> getRevenueById(@PathVariable Long id) {
        log.info("Endpoint: GET /revenues/{} - Get revenue by ID", id);

        RevenueDTO revenue = revenueService.getRevenueById(id);
        return ResponseEntity.ok(ApiResponse.success(revenue, "Revenue retrieved successfully"));
    }

    /**
     * GET /api/revenues/year/{year}/month/{month}
     * Get revenue by year and month
     */
    @GetMapping("/year/{year}/month/{month}")
    public ResponseEntity<ApiResponse<RevenueDTO>> getRevenueByYearAndMonth(
            @PathVariable Integer year,
            @PathVariable Integer month) {
        log.info("Endpoint: GET /revenues/year/{}/month/{} - Get revenue by year and month", year, month);

        RevenueDTO revenue = revenueService.getRevenueByYearAndMonth(year, month);
        return ResponseEntity.ok(ApiResponse.success(revenue, "Revenue retrieved successfully"));
    }

    /**
     * GET /api/revenues
     * Get all revenues with pagination
     * Query Parameters:
     * - page: Page number (0-based, default: 0)
     * - size: Page size (default: 20)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<RevenueDTO>>> getAllRevenues(Pageable pageable) {
        log.info("Endpoint: GET /revenues - Get all revenues");

        Page<RevenueDTO> revenues = revenueService.getAllRevenues(pageable);
        return ResponseEntity.ok(ApiResponse.success(revenues, "Revenues retrieved successfully"));
    }

    /**
     * GET /api/revenues/by-year/{year}
     * Get revenues by year with pagination
     * Query Parameters:
     * - page: Page number (0-based, default: 0)
     * - size: Page size (default: 20)
     */
    @GetMapping("/by-year/{year}")
    public ResponseEntity<ApiResponse<Page<RevenueDTO>>> getRevenuesByYear(
            @PathVariable Integer year,
            Pageable pageable) {
        log.info("Endpoint: GET /revenues/by-year/{} - Get revenues by year", year);

        Page<RevenueDTO> revenues = revenueService.getRevenuesByYear(year, pageable);
        return ResponseEntity.ok(ApiResponse.success(revenues, "Revenues retrieved successfully"));
    }

    /**
     * DELETE /api/revenues/{id}
     * Delete (soft delete) a revenue
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRevenue(@PathVariable Long id) {
        log.info("Endpoint: DELETE /revenues/{} - Delete revenue", id);

        revenueService.deleteRevenue(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Revenue deleted successfully"));
    }

    /**
     * GET /api/revenues/summary/year/{year}/month/{month}
     * Get revenue summary with salaries and orders for a specific month
     */
    @GetMapping("/summary/year/{year}/month/{month}")
    public ResponseEntity<ApiResponse<Object>> getRevenueSummary(
            @PathVariable Integer year,
            @PathVariable Integer month) {
        log.info("Endpoint: GET /revenues/summary/year/{}/month/{} - Get revenue summary", year, month);

        Object summary = revenueService.getRevenueSummary(year, month);
        return ResponseEntity.ok(ApiResponse.success(summary, "Revenue summary retrieved successfully"));
    }
}

