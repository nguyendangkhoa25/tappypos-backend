package com.knp.model.dto.pawn;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PawnVehicleDetail {
    private String brand;
    private String model;
    private Integer year;
    private String licensePlate;
    private String engineNumber;
    private String chassisNumber;
    private String color;
    private String condition;
}
