package com.tappy.pos.model.dto.consignment;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Settle-by-sales preview for a consignment over a date range. */
@Data
@Builder
public class ConsignmentSettlementDTO {
    private Long consignmentId;
    private String placementNumber;
    private String publisherName;
    private LocalDate from;
    private LocalDate to;
    private Integer totalQuantitySold;
    private BigDecimal totalAmountDue;
    private List<ConsignmentSettlementLineDTO> lines;
}
