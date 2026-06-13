package com.tappy.pos.service.report;

import com.tappy.pos.model.dto.report.EndOfDayReportDTO;

import java.time.LocalDate;

public interface EndOfDayReportService {

    /** Daily cash summary (gold sold/bought + pawn) for the given day; defaults to today when null. */
    EndOfDayReportDTO getEndOfDay(LocalDate date);
}
