package com.tappy.pos.service.scheduler;

import com.tappy.pos.model.entity.appointment.Appointment;
import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.repository.appointment.AppointmentRepository;
import com.tappy.pos.repository.order.OrderRepository;
import com.tappy.pos.repository.pawn.PawnRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.messaging.TappyMessageClient;
import com.tappy.pos.service.notification.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SchedulerNotificationHelper Unit Tests")
class SchedulerNotificationHelperTest {

    @Mock private OrderRepository orderRepository;
    @Mock private PawnRepository pawnRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private NotificationService notificationService;
    @Mock private MessageService messageService;
    @Mock private TappyMessageClient tappyMessageClient;

    @InjectMocks
    private SchedulerNotificationHelper helper;

    private Tenant tenant() {
        Tenant t = org.mockito.Mockito.mock(Tenant.class);
        lenient().when(t.getTenantId()).thenReturn("shop-1");
        return t;
    }

    @Test
    @DisplayName("sendDailyRevenueSummary: pushes a summary when there are completed orders")
    void dailyRevenue_withOrders() {
        when(orderRepository.sumRevenueByDateRange(any(), any())).thenReturn(new BigDecimal("1500000"));
        when(orderRepository.countByDateRange(any(), any())).thenReturn(5L);

        helper.sendDailyRevenueSummary(tenant());

        verify(notificationService).pushToRoles(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("sendDailyRevenueSummary: no orders → no notification")
    void dailyRevenue_noOrders() {
        when(orderRepository.sumRevenueByDateRange(any(), any())).thenReturn(BigDecimal.ZERO);
        when(orderRepository.countByDateRange(any(), any())).thenReturn(0L);

        helper.sendDailyRevenueSummary(tenant());

        verify(notificationService, never()).pushToRoles(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("sendPawnDueNotification: pushes when contracts are due today")
    void pawnDue_withDue() {
        when(pawnRepository.sumByPawnStatusAndPawnDueDateBetween(any(), any(), any(), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenReturn(List.<Object[]>of(new Object[]{new BigDecimal("5000000"), 3L}));

        helper.sendPawnDueNotification(tenant());

        verify(notificationService).pushToRoles(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("sendPawnDueNotification: nothing due → no notification")
    void pawnDue_empty() {
        when(pawnRepository.sumByPawnStatusAndPawnDueDateBetween(any(), any(), any(), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenReturn(List.of());

        helper.sendPawnDueNotification(tenant());

        verify(notificationService, never()).pushToRoles(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("sendPawnDueNotification: reminds each borrower whose contract is upcoming-due via the message client")
    void pawnDue_borrowerReminder_sends() {
        when(pawnRepository.sumByPawnStatusAndPawnDueDateBetween(any(), any(), any(), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenReturn(List.of());
        when(pawnRepository.findDueForCustomerReminder(any(), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{
                        7L, "Khách B", "0900000001",
                        LocalDate.of(2026, 6, 23).atStartOfDay(), new BigDecimal("5000000")}));

        helper.sendPawnDueNotification(tenant());

        verify(tappyMessageClient).sendPawnDueReminder(
                anyString(), anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("sendPawnDueNotification: no upcoming-due contracts → no borrower reminder")
    void pawnDue_borrowerReminder_noneDue_skips() {
        when(pawnRepository.sumByPawnStatusAndPawnDueDateBetween(any(), any(), any(), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenReturn(List.of());
        when(pawnRepository.findDueForCustomerReminder(any(), any(), any()))
                .thenReturn(List.of());

        helper.sendPawnDueNotification(tenant());

        verify(tappyMessageClient, never()).sendPawnDueReminder(
                any(), any(), any(), any(), anyLong());
    }

    @Test
    @DisplayName("sendAppointmentReminders: sends a reminder per due appointment and marks them sent")
    void appointmentReminders_withDue() {
        Appointment appt = Appointment.builder()
                .customerName("Khách A")
                .customerPhone("0900000000")
                .scheduledDate(LocalDate.of(2026, 6, 1))
                .scheduledStartTime(LocalTime.of(10, 0))
                .services(new ArrayList<>())
                .build();
        appt.setId(1L);
        when(appointmentRepository.findDueForReminder(anyString(), any(), any(), any()))
                .thenReturn(List.of(appt));

        helper.sendAppointmentReminders(tenant(), LocalTime.of(10, 0));

        verify(tappyMessageClient).sendAppointmentReminder(
                any(), any(), any(), any(), any(), anyLong());
        verify(appointmentRepository).saveAll(any());
    }

    @Test
    @DisplayName("sendAppointmentReminders: nothing due → no message call")
    void appointmentReminders_empty() {
        when(appointmentRepository.findDueForReminder(anyString(), any(), any(), any()))
                .thenReturn(List.of());

        helper.sendAppointmentReminders(tenant(), LocalTime.of(10, 0));

        verify(tappyMessageClient, never()).sendAppointmentReminder(
                any(), any(), any(), any(), any(), anyLong());
    }
}
