package com.tappy.pos.model.dto.appointment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentDTO {
    private Long id;
    private String appointmentNumber;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private LocalDate scheduledDate;
    private LocalTime scheduledStartTime;
    private Integer durationMinutes;
    private String status;
    private String note;
    private Long linkedOrderId;
    private String createdBy;
    private LocalDateTime createdAt;
    private List<AppointmentServiceItemDTO> services;
}
