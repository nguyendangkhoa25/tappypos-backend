package com.knp.model.dto.pawn;

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
    private int newRequestMoneyCount;
    private long newRequestMoneyAmount;
    private long interestPawnAmount;
}
