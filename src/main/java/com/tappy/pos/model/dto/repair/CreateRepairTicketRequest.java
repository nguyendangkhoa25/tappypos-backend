package com.tappy.pos.model.dto.repair;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateRepairTicketRequest {
    private Long customerId;

    @NotBlank
    private String customerName;

    private String customerPhone;

    private String deviceType;
    private String brand;
    private String model;
    private String serialImei;

    @NotBlank
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
