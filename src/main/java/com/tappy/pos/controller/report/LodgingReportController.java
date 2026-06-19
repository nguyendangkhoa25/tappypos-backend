package com.tappy.pos.controller.report;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.report.LodgingReportDTO;
import com.tappy.pos.service.report.LodgingReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/reports/lodging")
@RequiredArgsConstructor
@RequiresFeature("ROOM")
public class LodgingReportController {

    private final LodgingReportService lodgingReportService;

    @GetMapping
    public ResponseEntity<ApiResponse<LodgingReportDTO>> getLodgingReport(
            @RequestParam(defaultValue = "30") int days) {
        log.info("GET /reports/lodging?days={}", days);
        return ResponseEntity.ok(ApiResponse.success(lodgingReportService.getLodgingReport(days)));
    }
}
