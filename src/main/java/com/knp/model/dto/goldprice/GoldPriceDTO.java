package com.knp.model.dto.goldprice;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoldPriceDTO {
    private Long id;
    private String code;
    private String label;
    private BigDecimal buy;
    private BigDecimal sell;
    private BigDecimal pawn;
    private int displayOrder;
    private String note;
    private boolean showInBoard;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
