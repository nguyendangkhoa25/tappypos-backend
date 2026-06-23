package com.tappy.pos.model.dto.consignment;

import com.tappy.pos.model.enums.ConsignmentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ConsignmentDTO {
    private Long id;
    private Long publisherId;
    private String publisherName;
    private String placementNumber;
    private LocalDate placementDate;
    private ConsignmentStatus status;
    private String statusDisplayName;
    private String note;
    private LocalDate settledFrom;
    private LocalDate settledTo;
    private LocalDateTime settledDate;
    private BigDecimal settledAmount;
    private Integer totalQuantityPlaced;
    private String createdBy;
    private LocalDateTime createdAt;
    private List<ConsignmentItemDTO> items;
}
