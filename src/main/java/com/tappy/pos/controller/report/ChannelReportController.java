package com.tappy.pos.controller.report;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.report.ChannelRevenueDTO;
import com.tappy.pos.service.report.ChannelReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/reports/order-channels")
@RequiredArgsConstructor
@RequiresFeature("REVENUE")
public class ChannelReportController {

    private final ChannelReportService channelReportService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ChannelRevenueDTO>>> getRevenueByChannel(
            @RequestParam(defaultValue = "30") int days) {
        log.info("GET /reports/order-channels?days={}", days);
        return ResponseEntity.ok(ApiResponse.success(channelReportService.getRevenueByChannel(days)));
    }
}
