package com.tappy.pos.model.dto.appointment;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class UpdateAppointmentRequest {
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private LocalDate scheduledDate;
    private LocalTime scheduledStartTime;
    private Integer durationMinutes;
    private String note;
    private List<AppointmentServiceRequest> services;
}
