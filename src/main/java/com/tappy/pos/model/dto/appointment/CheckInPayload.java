package com.tappy.pos.model.dto.appointment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckInPayload {
    private Long appointmentId;
    private String appointmentNumber;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private List<AppointmentServiceItemDTO> services;
}
