package com.tappy.pos.model.dto.appointment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class CreateAppointmentRequest {

    private Long customerId;

    @NotBlank
    private String customerName;

    private String customerPhone;

    @NotNull
    private LocalDate scheduledDate;

    @NotNull
    private LocalTime scheduledStartTime;

    private Integer durationMinutes = 60;

    private String note;

    @Valid
    private List<AppointmentServiceRequest> services;
}
