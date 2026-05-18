package com.tappy.pos.model.dto.pawn;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PawnRealEstateDetail {
    private String certificateNumber;
    private String certificateType;
    private String ownerName;
    private String address;
    private BigDecimal areaSqm;
    private String condition;
}
