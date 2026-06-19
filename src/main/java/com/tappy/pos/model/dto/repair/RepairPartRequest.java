package com.tappy.pos.model.dto.repair;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RepairPartRequest {
    private Long productId;

    @NotBlank
    private String productName;

    private Integer quantity;
    private BigDecimal unitPrice;
}
