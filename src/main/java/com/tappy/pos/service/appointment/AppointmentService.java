package com.tappy.pos.service.appointment;

import com.tappy.pos.model.dto.appointment.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface AppointmentService {
    Page<AppointmentDTO> getByDate(LocalDate date, Pageable pageable);
    AppointmentDTO getById(Long id);
    AppointmentDTO create(CreateAppointmentRequest request);
    AppointmentDTO update(Long id, UpdateAppointmentRequest request);
    AppointmentDTO confirm(Long id);
    CheckInPayload checkIn(Long id);
    AppointmentDTO cancel(Long id);
    AppointmentDTO noShow(Long id);
    void delete(Long id);
}
