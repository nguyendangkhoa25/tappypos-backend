package com.tappy.pos.model.dto.pawn;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DateFilterRequest {
    private long fromDate;
    private long toDate;
    private long equalDate;
    private String type;
}
