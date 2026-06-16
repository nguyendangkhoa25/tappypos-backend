package com.tappy.pos.service.report;

import com.tappy.pos.model.dto.report.CashDrawerDTO;
import com.tappy.pos.model.dto.report.CloseDrawerRequest;

import java.time.LocalDate;

public interface CashDrawerService {

    /** Live reconciliation preview for a day (opening carry-over, expected cash, and any saved close). */
    CashDrawerDTO getReconciliation(LocalDate date);

    /** Reconcile and persist the day's close (upsert by business date). */
    CashDrawerDTO close(CloseDrawerRequest request);
}
