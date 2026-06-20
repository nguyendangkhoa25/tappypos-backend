package com.tappy.pos.model.dto.repair;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RepairTicketDTO {
    private Long id;
    private String ticketNumber;

    private Long customerId;
    private String customerName;
    private String customerPhone;

    private String deviceType;
    private String brand;
    private String model;
    private String serialImei;

    private String reportedFault;
    private String diagnosis;
    private BigDecimal quoteAmount;
    private BigDecimal partsAmount;
    private BigDecimal laborAmount;
    private BigDecimal totalAmount;
    private Integer warrantyDays;

    private Long assignedTechnicianId;
    private String assignedTechnicianName;
    private String status;
    private Boolean isWarrantyClaim;
    private String note;

    private LocalDateTime receivedAt;
    private LocalDateTime completedAt;
    private LocalDateTime deliveredAt;
    private Long linkedOrderId;

    /** When the repair warranty lapses (deliveredAt + warrantyDays); null if not delivered or no warranty. */
    private LocalDateTime warrantyExpiresAt;
    /** True when the device is still inside its repair-warranty window right now. */
    private Boolean underWarranty;

    private String createdBy;
    private LocalDateTime createdAt;

    private List<RepairPartDTO> parts;
}
