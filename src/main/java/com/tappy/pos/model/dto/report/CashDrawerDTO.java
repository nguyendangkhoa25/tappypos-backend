package com.tappy.pos.model.dto.report;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Cash-drawer reconciliation for a business day: expected physical cash vs. counted. */
@Data
@Builder
public class CashDrawerDTO {

    private LocalDate businessDate;

    private BigDecimal opening;        // carried over from the previous day's count (editable)
    private BigDecimal cashSales;      // + cash SELL takings
    private BigDecimal pawnRedeemed;   // + pawn redemptions (principal + interest); 0 when pawn disabled
    private BigDecimal goldBuy;        // − cash paid buying gold
    private BigDecimal pawnLoans;      // − pawn loans disbursed; 0 when pawn disabled
    private BigDecimal cashExpenses;   // − cash expenses
    private BigDecimal expected;       // opening + ins − outs

    private boolean pawnEnabled;

    // ── Reconciliation result — null/false until the day is closed ──
    private boolean closed;
    private BigDecimal counted;        // physical cash counted by the owner
    private BigDecimal difference;     // counted − expected (positive = over, negative = short)
    private String closedBy;
    private LocalDateTime closedAt;
    private String note;
}
