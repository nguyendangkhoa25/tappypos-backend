package com.tappy.pos.model.dto.table;

import com.tappy.pos.model.enums.TableStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TableDTO {
    private Long id;
    private String tableNumber;
    private Integer capacity;
    private TableStatus status;
    private Long currentOrderId;
    private String currentOrderNumber;
    private String location;
    private Integer displayOrder;
    private Long elapsedMinutes;
}
