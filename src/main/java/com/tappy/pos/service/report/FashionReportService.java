package com.tappy.pos.service.report;

import com.tappy.pos.model.dto.report.FashionReportDTO;

public interface FashionReportService {

    /** Variant analytics over a trailing {@code days} window (best sellers, dead stock, sell-through). */
    FashionReportDTO getFashionReport(int days);
}
