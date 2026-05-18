package com.tappy.pos.model.dto.pawn;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PawnWatchDetail {
    private String brand;
    private String model;
    private String material;
    private String condition;
}
