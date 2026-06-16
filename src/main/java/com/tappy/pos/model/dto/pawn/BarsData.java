package com.tappy.pos.model.dto.pawn;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BarsData {
    /** Pre-formatted chart label — "YYYY-MM-DD" for day/week, "YYYY-MM" for month, "YYYY" for year */
    private String label;
    private int year;
    private int month;
    private int day;
    private long amount;
    private int count;
    private double weight;
}
