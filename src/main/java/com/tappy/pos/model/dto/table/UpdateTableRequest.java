package com.tappy.pos.model.dto.table;

import lombok.Data;

@Data
public class UpdateTableRequest {
    private String tableNumber;
    private Integer capacity;
    private String location;
    private Integer displayOrder;
}
