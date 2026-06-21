package com.tappy.pos.service.appointment;

import com.tappy.pos.service.audit.ActivityLogService;

import com.tappy.pos.config.AuthContext;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.appointment.AppointmentDTO;
import com.tappy.pos.model.dto.appointment.AppointmentServiceRequest;
import com.tappy.pos.model.dto.appointment.AppointmentWeekSummaryDTO;
import com.tappy.pos.model.dto.appointment.CheckInPayload;
import com.tappy.pos.model.dto.appointment.CreateAppointmentRequest;
import com.tappy.pos.model.dto.appointment.UpdateAppointmentRequest;
import com.tappy.pos.model.entity.appointment.Appointment;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.appointment.AppointmentRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.notification.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentServiceImpl Unit Tests")
class AppointmentServiceImplTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private TenantContext tenantContext;
    @Mock private NotificationService notificationService;
    @Mock private MessageService messageService;

    @Mock
    private AuthContext authContext;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private AppointmentServiceImpl service;

    private static final String TENANT = "shop-1";
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("cashier01", null, java.util.Collections.emptyList()));
        pageable = PageRequest.of(0, 20);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Appointment appointment(Long id, String status) {
        Appointment a = Appointment.builder()
                .tenantId(TENANT)
                .appointmentNumber("APT-1")
                .customerId(10L)
                .customerName("Khách A")
                .customerPhone("0900000000")
                .scheduledDate(LocalDate.of(2026, 6, 1))
                .scheduledStartTime(LocalTime.of(9, 0))
                .durationMinutes(60)
                .status(status)
                .services(new ArrayList<>())
                .build();
        a.setId(id);
        return a;
    }

    // ── reads ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getByDate: maps page of appointments")
    void getByDate() {
        when(tenantContext.getCurrentTenantId()).thenReturn(TENANT);
        when(appointmentRepository.findByTenantIdAndDate(eq(TENANT), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(appointment(1L, "PENDING"))));

        Page<AppointmentDTO> result = service.getByDate(LocalDate.of(2026, 6, 1), pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getByCustomer: maps page of appointments")
    void getByCustomer() {
        when(tenantContext.getCurrentTenantId()).thenReturn(TENANT);
        when(appointmentRepository.findByCustomerId(eq(TENANT), eq(10L), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(appointment(1L, "PENDING"))));

        Page<AppointmentDTO> result = service.getByCustomer(10L, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getById: returns the mapped appointment")
    void getById() {
        when(tenantContext.getCurrentTenantId()).thenReturn(TENANT);
        when(appointmentRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT))
                .thenReturn(Optional.of(appointment(1L, "CONFIRMED")));

        AppointmentDTO dto = service.getById(1L);

        assertThat(dto.getStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("getById: not found → ResourceNotFoundException")
    void getById_notFound() {
        when(tenantContext.getCurrentTenantId()).thenReturn(TENANT);
        when(appointmentRepository.findByIdAndTenantIdAndDeletedFalse(99L, TENANT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    // ── create / update ────────────────────────────────────────────────────────

    @Test
    @DisplayName("create: builds a PENDING appointment, generates number, pushes notification")
    void create_success() {
        when(tenantContext.getCurrentTenantId()).thenReturn(TENANT);
        when(appointmentRepository.countTodayByTenantId(eq(TENANT), any())).thenReturn(2L);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> {
            Appointment a = inv.getArgument(0);
            a.setId(100L);
            return a;
        });

        CreateAppointmentRequest req = new CreateAppointmentRequest();
        req.setCustomerName("  Khách B  ");
        req.setCustomerPhone("0911111111");
        req.setScheduledDate(LocalDate.of(2026, 6, 2));
        req.setScheduledStartTime(LocalTime.of(10, 0));
        req.setDurationMinutes(45);

        AppointmentDTO dto = service.create(req);

        assertThat(dto.getId()).isEqualTo(100L);
        assertThat(dto.getStatus()).isEqualTo("PENDING");
        assertThat(dto.getCustomerName()).isEqualTo("Khách B"); // trimmed
        assertThat(dto.getAppointmentNumber()).startsWith("APT-");
        verify(notificationService).pushToRolesAsync(any(), any(), any(), any(), any(), any(), eq(TENANT), eq("cashier01"));
    }

    @Test
    @DisplayName("create: overlapping employee booking yields a conflict warning")
    void create_withConflictWarning() {
        when(tenantContext.getCurrentTenantId()).thenReturn(TENANT);
        when(appointmentRepository.countTodayByTenantId(eq(TENANT), any())).thenReturn(0L);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> {
            Appointment a = inv.getArgument(0);
            a.setId(101L);
            return a;
        });
        // existing appointment for the same employee overlapping 10:00-10:30
        Appointment existing = appointment(7L, "CONFIRMED");
        existing.setScheduledStartTime(LocalTime.of(10, 0));
        existing.setDurationMinutes(30);
        when(appointmentRepository.findByEmployeeAndDate(eq(TENANT), eq(55L), any(), anyLong()))
                .thenReturn(List.of(existing));

        AppointmentServiceRequest svc = new AppointmentServiceRequest();
        svc.setProductName("Cắt tóc");
        svc.setAssignedEmployeeId(55L);
        svc.setAssignedEmployeeName("Thợ A");
        svc.setDurationMinutes(30);

        CreateAppointmentRequest req = new CreateAppointmentRequest();
        req.setCustomerName("Khách C");
        req.setScheduledDate(LocalDate.of(2026, 6, 2));
        req.setScheduledStartTime(LocalTime.of(10, 0));
        req.setDurationMinutes(30);
        req.setServices(List.of(svc));

        AppointmentDTO dto = service.create(req);

        assertThat(dto.getWarnings()).hasSize(1);
        assertThat(dto.getServices()).hasSize(1);
    }

    @Test
    @DisplayName("update: applies provided fields and replaces services")
    void update_success() {
        when(tenantContext.getCurrentTenantId()).thenReturn(TENANT);
        Appointment existing = appointment(1L, "PENDING");
        when(appointmentRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT)).thenReturn(Optional.of(existing));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateAppointmentRequest req = new UpdateAppointmentRequest();
        req.setCustomerName("  Tên Mới  ");
        req.setDurationMinutes(90);

        AppointmentDTO dto = service.update(1L, req);

        assertThat(dto.getCustomerName()).isEqualTo("Tên Mới");
        assertThat(dto.getDurationMinutes()).isEqualTo(90);
    }

    @Test
    @DisplayName("update: CHECKED_IN appointment cannot be edited")
    void update_notEditable() {
        when(tenantContext.getCurrentTenantId()).thenReturn(TENANT);
        when(appointmentRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT))
                .thenReturn(Optional.of(appointment(1L, "CHECKED_IN")));
        when(messageService.getMessage("error.appointment.edit.invalid.status")).thenReturn("không thể sửa");

        assertThatThrownBy(() -> service.update(1L, new UpdateAppointmentRequest()))
                .isInstanceOf(BadRequestException.class);
        verify(appointmentRepository, never()).save(any());
    }

    // ── status transitions ───────────────────────────────────────────────────

    @Test
    @DisplayName("confirm: PENDING → CONFIRMED")
    void confirm_success() {
        when(tenantContext.getCurrentTenantId()).thenReturn(TENANT);
        Appointment a = appointment(1L, "PENDING");
        when(appointmentRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT)).thenReturn(Optional.of(a));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        AppointmentDTO dto = service.confirm(1L);

        assertThat(dto.getStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("confirm: non-PENDING → BadRequestException")
    void confirm_invalidStatus() {
        when(tenantContext.getCurrentTenantId()).thenReturn(TENANT);
        when(appointmentRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT))
                .thenReturn(Optional.of(appointment(1L, "CONFIRMED")));
        when(messageService.getMessage("error.appointment.confirm.invalid.status")).thenReturn("sai trạng thái");

        assertThatThrownBy(() -> service.confirm(1L)).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("checkIn: sets CHECKED_IN and returns payload with services")
    void checkIn_success() {
        when(tenantContext.getCurrentTenantId()).thenReturn(TENANT);
        Appointment a = appointment(1L, "CONFIRMED");
        when(appointmentRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT)).thenReturn(Optional.of(a));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        CheckInPayload payload = service.checkIn(1L);

        assertThat(a.getStatus()).isEqualTo("CHECKED_IN");
        assertThat(payload.getAppointmentId()).isEqualTo(1L);
        assertThat(payload.getCustomerName()).isEqualTo("Khách A");
    }

    @Test
    @DisplayName("checkIn: CANCELLED appointment cannot check in")
    void checkIn_invalidStatus() {
        when(tenantContext.getCurrentTenantId()).thenReturn(TENANT);
        when(appointmentRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT))
                .thenReturn(Optional.of(appointment(1L, "CANCELLED")));
        when(messageService.getMessage("error.appointment.checkin.invalid.status")).thenReturn("không thể");

        assertThatThrownBy(() -> service.checkIn(1L)).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("cancel: sets CANCELLED and pushes notification")
    void cancel_success() {
        when(tenantContext.getCurrentTenantId()).thenReturn(TENANT);
        Appointment a = appointment(1L, "CONFIRMED");
        when(appointmentRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT)).thenReturn(Optional.of(a));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        AppointmentDTO dto = service.cancel(1L);

        assertThat(dto.getStatus()).isEqualTo("CANCELLED");
        verify(notificationService).pushToRolesAsync(any(), any(), any(), any(), any(), any(), eq(TENANT), eq(null));
    }

    @Test
    @DisplayName("noShow: sets NO_SHOW")
    void noShow_success() {
        when(tenantContext.getCurrentTenantId()).thenReturn(TENANT);
        Appointment a = appointment(1L, "CONFIRMED");
        when(appointmentRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT)).thenReturn(Optional.of(a));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        AppointmentDTO dto = service.noShow(1L);

        assertThat(dto.getStatus()).isEqualTo("NO_SHOW");
    }

    @Test
    @DisplayName("noShow: already CANCELLED → BadRequestException")
    void noShow_alreadyCancelled() {
        when(tenantContext.getCurrentTenantId()).thenReturn(TENANT);
        when(appointmentRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT))
                .thenReturn(Optional.of(appointment(1L, "CANCELLED")));
        when(messageService.getMessage("error.appointment.noshow.already.cancelled")).thenReturn("đã hủy");

        assertThatThrownBy(() -> service.noShow(1L)).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("delete: soft-deletes and saves")
    void delete_success() {
        when(tenantContext.getCurrentTenantId()).thenReturn(TENANT);
        Appointment a = appointment(1L, "PENDING");
        when(appointmentRepository.findByIdAndTenantIdAndDeletedFalse(1L, TENANT)).thenReturn(Optional.of(a));

        service.delete(1L);

        verify(appointmentRepository).save(a);
    }

    // ── analytics ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAnalytics: summary, day trend and rankings")
    void getAnalytics_day() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 10);
        when(appointmentRepository.getAnalyticsSummary(from, to)).thenReturn(new Object[]{10L, 7L, 2L});
        when(appointmentRepository.getAnalyticsTrendByDay(from, to))
                .thenReturn(List.<Object[]>of(new Object[]{"2026-06-01", 3L, 2L, 0L}));
        when(appointmentRepository.getServiceRanking(eq(from), eq(to), anyInt()))
                .thenReturn(List.<Object[]>of(new Object[]{"Cắt tóc", 5L}));
        when(appointmentRepository.getEmployeeRanking(eq(from), eq(to), anyInt()))
                .thenReturn(List.<Object[]>of(new Object[]{"Thợ A", 4L}));

        Map<String, Object> result = service.getAnalytics(from, to, "day", 5);

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) result.get("summary");
        assertThat(summary.get("total")).isEqualTo(10L);
        assertThat(summary.get("completedCount")).isEqualTo(7L);
        assertThat(summary.get("completionRate")).isEqualTo(0.7);
        assertThat((List<?>) result.get("trend")).hasSize(1);
        assertThat((List<?>) result.get("rankingServices")).hasSize(1);
        assertThat((List<?>) result.get("rankingEmployees")).hasSize(1);
    }

    @Test
    @DisplayName("getAnalytics: week granularity uses weekly trend; empty summary handled")
    void getAnalytics_week_emptySummary() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 30);
        when(appointmentRepository.getAnalyticsSummary(from, to)).thenReturn(new Object[]{null, null, null});
        when(appointmentRepository.getAnalyticsTrendByWeek(from, to)).thenReturn(List.of());
        when(appointmentRepository.getServiceRanking(eq(from), eq(to), anyInt())).thenReturn(List.of());
        when(appointmentRepository.getEmployeeRanking(eq(from), eq(to), anyInt())).thenReturn(List.of());

        Map<String, Object> result = service.getAnalytics(from, to, "week", 5);

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) result.get("summary");
        assertThat(summary.get("total")).isEqualTo(0L);
        assertThat(summary.get("completionRate")).isEqualTo(0.0);
    }

    @Test
    @DisplayName("getWeekSummary: aggregates counts keyed by date")
    void getWeekSummary() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 7);
        when(tenantContext.getCurrentTenantId()).thenReturn(TENANT);
        when(appointmentRepository.countByDateRange(TENANT, from, to))
                .thenReturn(List.<Object[]>of(
                        new Object[]{LocalDate.of(2026, 6, 1), 3L},
                        new Object[]{LocalDate.of(2026, 6, 2), 5L}));

        AppointmentWeekSummaryDTO summary = service.getWeekSummary(from, to);

        assertThat(summary.getCountByDate()).containsEntry("2026-06-01", 3L);
        assertThat(summary.getCountByDate()).containsEntry("2026-06-02", 5L);
    }
}
