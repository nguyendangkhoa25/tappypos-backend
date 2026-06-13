package com.tappy.pos.model.dto.report;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Owner's end-of-day cash summary for a gold-trading / pawn shop. */
@Data
@Builder
public class EndOfDayReportDTO {

    private LocalDate date;

    /** Gold/silver sold to customers (cash in). */
    private GoldLine goldSold;
    /** Gold/silver bought from customers (cash out). */
    private GoldLine goldBought;

    /** True when the shop has the PAWN feature — drives whether the pawn rows are shown. */
    private boolean pawnEnabled;
    /** New pawn contracts today (cash out). Null when pawn is disabled. */
    private PawnNew pawnNew;
    /** Redemptions today — principal + interest collected (cash in). Null when pawn is disabled. */
    private PawnRedeemed pawnRedeemed;

    private Totals totals;

    @Data
    @Builder
    public static class GoldLine {
        private long count;
        private BigDecimal weightChi;
        private BigDecimal amount;
    }

    @Data
    @Builder
    public static class PawnNew {
        private long count;
        private BigDecimal amount;
    }

    @Data
    @Builder
    public static class PawnRedeemed {
        private BigDecimal amount;    // principal returned
        private BigDecimal interest;  // interest collected
    }

    @Data
    @Builder
    public static class Totals {
        private BigDecimal cashIn;
        private BigDecimal cashOut;
        private BigDecimal net;
    }
}
