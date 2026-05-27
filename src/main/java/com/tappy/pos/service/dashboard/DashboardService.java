package com.tappy.pos.service.dashboard;

import com.tappy.pos.model.dto.dashboard.DashboardKpiDTO;
import com.tappy.pos.model.dto.dashboard.DashboardSummaryDTO;

import java.time.LocalDateTime;

public interface DashboardService {

    DashboardSummaryDTO getSummary();

    DashboardKpiDTO getKpi(LocalDateTime from, LocalDateTime to, String fromDate, String toDate);
}
