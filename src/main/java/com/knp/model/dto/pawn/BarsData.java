package com.knp.model.dto.pawn;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BarsData {
    private int year;
    private int month;
    private long amount;
    private int count;
    private double weight;
}
