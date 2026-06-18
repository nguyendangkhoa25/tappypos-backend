package com.tappy.pos.service.pawn;

import com.tappy.pos.model.dto.pawn.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface PawnService {
    PawnResponse createPawn(PawnRequest pawnRequest);

    PawnSearchResponse getPawns(Pageable pageable, SearchPawnRequest searchRequest);

    PawnResponse updatePawn(Long pawnId, PawnRequest pawnRequest);

    PawnResponse getPawnDetails(Long pawnId);

    void deletePawnByPawnIds(List<Long> pawnIds);

    PawnResponse cancelPawnByPawnId(Long pawnId, String cancelReason);

    PawnResponse forfeitPawnByPawnId(Long pawnId, ForfeitRequest forfeitRequest);

    PawnResponse calculatePawnRedeem(Long pawnId, RedeemRequest redeemRequest);

    ReqMoneyResponse requestMoreMoney(Long pawnId, ReqMoneyRequest reqMoneyRequest);

    PawnResponse extendPawn(Long pawnId, PawnRequest pawnRequest);

    PawnKPIs getPawnKPIs(DateFilterRequest dateFilter);

    FileSystemResource exportPawns(SearchPawnRequest searchRequest) throws IOException;

    PawnBarsResponse getPawnCharts(DateFilterRequest dateFilter);

    int updateVisibleStatus(Long pawnId, boolean visibleStatus);

    List<Long> getPawnIdsToClean(Pageable pageable, SearchPawnRequest searchRequest);

    PawnSetting updatePawnSetting(PawnSetting setting);

    PawnSetting getPawnSetting();

    List<Map<String, Object>> getTopPawnCustomers(int limit, LocalDate from, LocalDate to);

    PawnCustomerInsights getPawnCustomerInsights(LocalDate from, LocalDate to);

    PawnResponse lookupByCode(String code);

    Map<String, Object> getCustomerPawnSummary(Long customerId);

    /**
     * Per-customer pawn KPI rankings for the given date range. Returns a map with five lists —
     * topPawnedAmount, topPawnedCount, topCompletedPawnAmount, topCompletedPawnCount, topInterestAmount —
     * each a ranking of customers used by the customer pawn-KPI dashboard widget.
     */
    Map<String, Object> getCustomerPawnKpi(DateFilterRequest filter);
}
