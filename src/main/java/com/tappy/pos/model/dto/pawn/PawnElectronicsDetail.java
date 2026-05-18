package com.tappy.pos.model.dto.pawn;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PawnElectronicsDetail {
    private String brand;
    private String model;
    private String imei;
    private String storage;
    private String color;
    private String condition;
}
