package com.knp.model.dto.pawn;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CustomerPawnKPIs {
    private List<CustomerPawnKPI> topCompletedPawnCount;
    private List<CustomerPawnKPI> topCompletedPawnAmount;
    private List<CustomerPawnKPI> topInterestAmount;
    private List<CustomerPawnKPI> topPawnedCount;
    private List<CustomerPawnKPI> topPawnedAmount;
}
