package com.tappy.pos.model.dto.appointment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AppointmentServiceRequest {

    @NotNull
    private Long productId;

    @NotBlank
    private String productName;

    private BigDecimal unitPrice = BigDecimal.ZERO;

    private Integer durationMinutes = 0;

    private Long assignedEmployeeId;

    private String assignedEmployeeName;
}
