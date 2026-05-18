package com.tappy.pos.model.dto.table;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CreateTableRequest {
    @NotBlank
    private String tableNumber;
    @Positive
    private Integer capacity = 4;
    private String location;
    private Integer displayOrder = 0;
}
