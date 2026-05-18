package com.tappy.pos.model.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardKpiDTO {
    private Long itemsSold;
    private BigDecimal revenue;
    private Long newPawnContracts;
    private BigDecimal newPawnAmount;
    private Long interestEarned;
    private String fromDate;
    private String toDate;
}
