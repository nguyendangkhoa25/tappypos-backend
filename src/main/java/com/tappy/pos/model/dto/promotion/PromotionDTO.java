package com.tappy.pos.model.dto.promotion;

import com.tappy.pos.model.enums.DiscountType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PromotionDTO {
    private Long id;
    private String name;
    private String code;
    private DiscountType type;
    private BigDecimal value;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer usageLimit;
    private Integer usedCount;
    private Boolean isActive;
    private String description;
    private LocalDateTime createdAt;
}
