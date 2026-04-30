package com.knp.model.dto.pawn;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DateFilterRequest {
    private long fromDate;
    private long toDate;
    private long equalDate;
    private String type;
}
