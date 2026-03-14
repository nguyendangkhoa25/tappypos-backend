package com.knp.model.dto.promotion;

import com.knp.model.enums.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SavePromotionRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String code;

    @NotNull
    private DiscountType type;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal value;

    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer usageLimit;
    private Boolean isActive;
    private String description;
}
