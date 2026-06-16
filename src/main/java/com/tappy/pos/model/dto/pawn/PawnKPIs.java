package com.tappy.pos.model.dto.pawn;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PawnKPIs {
    private int totalPawnedCount;
    private long totalPawnedAmount;
    private int dueTodayCount;
    private long dueTodayAmount;
    private int overdueCount;
    private long overdueAmount;
    private int newPawnsCount;
    private long newPawnsAmount;
    private int completedPawnCount;
    private long completedPawnAmount;
    private int forfeitedPawnCount;
    private long forfeitedPawnAmount;
    private int extendedPawnCount;
    private long extendedPawnAmount;
    private int newRequestMoneyCount;
    private long newRequestMoneyAmount;
    private long interestPawnAmount;
    private int upcomingCount;
    private long upcomingAmount;
    /** SUM(pawnAmount) only for REDEEMED+FORFEITED contracts closed in the period — excludes additional draws. Used for avgLoan on closed cohort. */
    private long closedPawnPureAmount;
}
