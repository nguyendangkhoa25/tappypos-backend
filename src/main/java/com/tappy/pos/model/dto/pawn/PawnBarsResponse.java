package com.tappy.pos.model.dto.pawn;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PawnBarsResponse {
    private List<BarsData> pawnedBars;
    private List<BarsData> redeemBars;
    private List<BarsData> forfeitedBars;
    private List<BarsData> interestBars;
}
