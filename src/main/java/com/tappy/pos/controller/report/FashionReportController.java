package com.tappy.pos.controller.report;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.report.FashionReportDTO;
import com.tappy.pos.service.report.FashionReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/reports/fashion")
@RequiredArgsConstructor
@RequiresFeature("DASHBOARD")
public class FashionReportController {

    private final FashionReportService fashionReportService;

    @GetMapping
    public ResponseEntity<ApiResponse<FashionReportDTO>> getFashionReport(
            @RequestParam(defaultValue = "30") int days) {
        log.info("GET /reports/fashion?days={}", days);
        return ResponseEntity.ok(ApiResponse.success(fashionReportService.getFashionReport(days)));
    }
}
