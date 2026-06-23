package com.tappy.pos.model.dto.repair;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Partial update — only non-null fields are applied. {@code parts}, when non-null,
 * replaces the full parts list.
 */
@Data
public class UpdateRepairTicketRequest {
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
    private BigDecimal laborAmount;
    private Integer warrantyDays;

    private Long assignedTechnicianId;
    private String assignedTechnicianName;
    private Boolean isWarrantyClaim;
    private String note;

    private List<RepairPartRequest> parts;
}
