package com.tappy.pos.model.dto.promotion;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ApplyPromotionResponse {
    private String code;
    private String name;
    private BigDecimal discountAmount;
    private String message;
}
