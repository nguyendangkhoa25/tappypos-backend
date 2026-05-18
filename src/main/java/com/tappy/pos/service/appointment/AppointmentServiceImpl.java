package com.tappy.pos.service.appointment;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.appointment.*;
import com.tappy.pos.model.entity.appointment.Appointment;
import com.tappy.pos.model.entity.appointment.AppointmentServiceItem;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.appointment.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final TenantContext tenantContext;

    @Override
    public Page<AppointmentDTO> getByDate(LocalDate date, Pageable pageable) {
        String tenantId = tenantContext.getCurrentTenantId();
        return appointmentRepository.findByTenantIdAndDate(tenantId, date, pageable)
                .map(this::mapToDTO);
    }

    @Override
    public AppointmentDTO getById(Long id) {
        return mapToDTO(findOrThrow(id));
    }

    @Override
    @Transactional
    public AppointmentDTO create(CreateAppointmentRequest request) {
        String tenantId = tenantContext.getCurrentTenantId();
        String username = currentUsername();

        String number = generateNumber(tenantId);

        Appointment appointment = Appointment.builder()
                .tenantId(tenantId)
                .appointmentNumber(number)
                .customerId(request.getCustomerId())
                .customerName(request.getCustomerName().trim())
                .customerPhone(request.getCustomerPhone())
                .scheduledDate(request.getScheduledDate())
                .scheduledStartTime(request.getScheduledStartTime())
                .durationMinutes(request.getDurationMinutes() != null ? request.getDurationMinutes() : 60)
                .status("PENDING")
                .note(request.getNote())
                .createdBy(username)
                .services(new ArrayList<>())
                .build();

        if (request.getServices() != null) {
            for (AppointmentServiceRequest svc : request.getServices()) {
                AppointmentServiceItem item = buildServiceItem(appointment, svc);
                appointment.getServices().add(item);
            }
        }

        return mapToDTO(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional
    public AppointmentDTO update(Long id, UpdateAppointmentRequest request) {
        Appointment appointment = findOrThrow(id);
        assertEditable(appointment);

        if (request.getCustomerId() != null) appointment.setCustomerId(request.getCustomerId());
        if (request.getCustomerName() != null) appointment.setCustomerName(request.getCustomerName().trim());
        if (request.getCustomerPhone() != null) appointment.setCustomerPhone(request.getCustomerPhone());
        if (request.getScheduledDate() != null) appointment.setScheduledDate(request.getScheduledDate());
        if (request.getScheduledStartTime() != null) appointment.setScheduledStartTime(request.getScheduledStartTime());
        if (request.getDurationMinutes() != null) appointment.setDurationMinutes(request.getDurationMinutes());
        if (request.getNote() != null) appointment.setNote(request.getNote());

        if (request.getServices() != null) {
            appointment.getServices().clear();
            for (AppointmentServiceRequest svc : request.getServices()) {
                appointment.getServices().add(buildServiceItem(appointment, svc));
            }
        }

        return mapToDTO(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional
    public AppointmentDTO confirm(Long id) {
        Appointment appointment = findOrThrow(id);
        if (!"PENDING".equals(appointment.getStatus())) {
            throw new BadRequestException("Chỉ có thể xác nhận lịch hẹn ở trạng thái chờ xác nhận");
        }
        appointment.setStatus("CONFIRMED");
        return mapToDTO(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional
    public CheckInPayload checkIn(Long id) {
        Appointment appointment = findOrThrow(id);
        if ("CANCELLED".equals(appointment.getStatus()) || "NO_SHOW".equals(appointment.getStatus())) {
            throw new BadRequestException("Không thể check-in lịch hẹn đã huỷ hoặc không đến");
        }
        appointment.setStatus("CHECKED_IN");
        appointmentRepository.save(appointment);

        List<AppointmentServiceItemDTO> services = appointment.getServices().stream()
                .map(this::mapServiceItemToDTO)
                .collect(Collectors.toList());

        return CheckInPayload.builder()
                .appointmentId(appointment.getId())
                .appointmentNumber(appointment.getAppointmentNumber())
                .customerId(appointment.getCustomerId())
                .customerName(appointment.getCustomerName())
                .customerPhone(appointment.getCustomerPhone())
                .services(services)
                .build();
    }

    @Override
    @Transactional
    public AppointmentDTO cancel(Long id) {
        Appointment appointment = findOrThrow(id);
        assertEditable(appointment);
        appointment.setStatus("CANCELLED");
        return mapToDTO(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional
    public AppointmentDTO noShow(Long id) {
        Appointment appointment = findOrThrow(id);
        if ("CANCELLED".equals(appointment.getStatus())) {
            throw new BadRequestException("Lịch hẹn đã huỷ không thể đánh dấu không đến");
        }
        appointment.setStatus("NO_SHOW");
        return mapToDTO(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Appointment appointment = findOrThrow(id);
        appointment.softDelete();
        appointmentRepository.save(appointment);
    }

    // ---- helpers ----

    private Appointment findOrThrow(Long id) {
        return appointmentRepository
                .findByIdAndTenantIdAndDeletedFalse(id, tenantContext.getCurrentTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch hẹn #" + id));
    }

    private void assertEditable(Appointment a) {
        if ("CHECKED_IN".equals(a.getStatus()) || "CANCELLED".equals(a.getStatus()) || "NO_SHOW".equals(a.getStatus())) {
            throw new BadRequestException("Không thể chỉnh sửa lịch hẹn ở trạng thái này");
        }
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private String generateNumber(String tenantId) {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = appointmentRepository.countTodayByTenantId(tenantId, LocalDate.now());
        return String.format("APT-%s-%03d", dateStr, seq);
    }

    private AppointmentServiceItem buildServiceItem(Appointment appointment, AppointmentServiceRequest svc) {
        return AppointmentServiceItem.builder()
                .appointment(appointment)
                .productId(svc.getProductId())
                .productName(svc.getProductName())
                .unitPrice(svc.getUnitPrice() != null ? svc.getUnitPrice() : java.math.BigDecimal.ZERO)
                .durationMinutes(svc.getDurationMinutes() != null ? svc.getDurationMinutes() : 0)
                .assignedEmployeeId(svc.getAssignedEmployeeId())
                .assignedEmployeeName(svc.getAssignedEmployeeName())
                .build();
    }

    private AppointmentDTO mapToDTO(Appointment a) {
        List<AppointmentServiceItemDTO> services = a.getServices() == null ? List.of()
                : a.getServices().stream().map(this::mapServiceItemToDTO).collect(Collectors.toList());

        return AppointmentDTO.builder()
                .id(a.getId())
                .appointmentNumber(a.getAppointmentNumber())
                .customerId(a.getCustomerId())
                .customerName(a.getCustomerName())
                .customerPhone(a.getCustomerPhone())
                .scheduledDate(a.getScheduledDate())
                .scheduledStartTime(a.getScheduledStartTime())
                .durationMinutes(a.getDurationMinutes())
                .status(a.getStatus())
                .note(a.getNote())
                .linkedOrderId(a.getLinkedOrderId())
                .createdBy(a.getCreatedBy())
                .createdAt(a.getCreatedAt())
                .services(services)
                .build();
    }

    private AppointmentServiceItemDTO mapServiceItemToDTO(AppointmentServiceItem s) {
        return AppointmentServiceItemDTO.builder()
                .id(s.getId())
                .productId(s.getProductId())
                .productName(s.getProductName())
                .unitPrice(s.getUnitPrice())
                .durationMinutes(s.getDurationMinutes())
                .assignedEmployeeId(s.getAssignedEmployeeId())
                .assignedEmployeeName(s.getAssignedEmployeeName())
                .build();
    }
}
