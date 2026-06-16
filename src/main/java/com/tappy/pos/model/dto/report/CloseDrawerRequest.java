package com.tappy.pos.model.dto.report;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Owner's request to close (reconcile) the cash drawer for a day. */
@Data
public class CloseDrawerRequest {

    /** Business day to close; null = today. */
    private LocalDate date;

    /** Opening float the owner confirmed (carry-over default, editable). Null treated as 0. */
    private BigDecimal opening;

    /** Physical cash counted in the drawer. */
    @NotNull
    private BigDecimal counted;

    @Size(max = 500)
    private String note;
}
