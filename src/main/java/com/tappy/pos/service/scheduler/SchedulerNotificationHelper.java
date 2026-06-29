package com.tappy.pos.service.scheduler;

import com.tappy.pos.model.entity.appointment.Appointment;
import com.tappy.pos.model.entity.appointment.AppointmentServiceItem;
import com.tappy.pos.model.entity.notification.Notification;
import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.model.enums.PawnStatus;
import com.tappy.pos.model.enums.RoleEnum;
import com.tappy.pos.repository.appointment.AppointmentRepository;
import com.tappy.pos.repository.order.OrderRepository;
import com.tappy.pos.repository.pawn.PawnRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.model.i18n.LocalizedText;
import com.tappy.pos.service.messaging.TappyMessageClient;
import com.tappy.pos.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Transactional helper for schedulers that need to query tenant-scoped data and push notifications.
 * Each public method starts a read-write transaction so TenantRlsAspect fires and sets the RLS policy
 * before any repository call.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SchedulerNotificationHelper {

    private final OrderRepository orderRepository;
    private final PawnRepository pawnRepository;
    private final AppointmentRepository appointmentRepository;
    private final NotificationService notificationService;
    private final MessageService messageService;
    private final TappyMessageClient tappyMessageClient;

    /** How many days before its due date a pawn contract triggers a borrower ZNS reminder. */
    @Value("${pawn.due.reminder-lead-days:3}")
    private int reminderLeadDays;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Queries today's completed orders and pushes a revenue summary to SHOP_OWNER.
     * TenantContext must be set by the caller before invoking this method.
     */
    @Transactional
    public void sendDailyRevenueSummary(Tenant tenant) {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = today.atTime(LocalTime.MAX);

        BigDecimal revenue = orderRepository.sumRevenueByDateRange(from, to);
        long count = orderRepository.countByDateRange(from, to);

        if (count == 0) {
            log.debug("No completed orders today for tenant {}", tenant.getTenantId());
            return;
        }

        String dateStr = today.format(DATE_FMT);
        String revenueStr = NumberFormat.getNumberInstance(new Locale("vi")).format(revenue) + " ₫";

        notificationService.pushToRoles(Notification.NotificationType.INFO,
                LocalizedText.of("notification.revenue.daily.title", dateStr),
                LocalizedText.of("notification.revenue.daily.message", count, revenueStr),
                "REVENUE", null, List.of(RoleEnum.SHOP_OWNER.getCode()));
        log.info("Daily revenue summary sent for tenant {}: {} orders, {}", tenant.getTenantId(), count, revenue);
    }

    /**
     * Pawn due-date reminders. Two independent parts:
     * <ol>
     *   <li>an in-app summary to SHOP_OWNER of contracts due <em>today</em>; and</li>
     *   <li>a Zalo ZNS reminder to each borrower whose contract falls due in
     *       {@code reminderLeadDays} days (so they have time to redeem or extend).</li>
     * </ol>
     * TenantContext must be set by the caller before invoking this method.
     */
    @Transactional
    public void sendPawnDueNotification(Tenant tenant) {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = today.atTime(LocalTime.MAX);

        List<Object[]> result = pawnRepository.sumByPawnStatusAndPawnDueDateBetween(
                PawnStatus.PAWNED, from, to, false);

        long count = result.isEmpty() ? 0 : ((Number) result.get(0)[1]).longValue();
        if (count > 0) {
            String dateStr = today.format(DATE_FMT);

            notificationService.pushToRoles(Notification.NotificationType.INFO,
                    LocalizedText.of("notification.pawn.due.title", count),
                    LocalizedText.of("notification.pawn.due.message", count, dateStr),
                    "PAWN", null, List.of(RoleEnum.SHOP_OWNER.getCode()));
            log.info("Pawn due notification sent for tenant {}: {} contract(s)", tenant.getTenantId(), count);
        }

        sendPawnDueCustomerReminders(tenant, today);
    }

    /**
     * Sends a Zalo reminder (via the Tappy Message service) to every borrower whose active contract
     * falls due exactly {@code reminderLeadDays} days from {@code today}. Each contract is reminded
     * once (a single due date hits the window on a single scheduler run). Fire-and-forget; the client
     * no-ops when no pawn-due-reminder template is configured.
     */
    private void sendPawnDueCustomerReminders(Tenant tenant, LocalDate today) {
        LocalDate target = today.plusDays(reminderLeadDays);
        LocalDateTime from = target.atStartOfDay();
        LocalDateTime to = target.atTime(LocalTime.MAX);

        List<Object[]> due = pawnRepository.findDueForCustomerReminder(PawnStatus.PAWNED, from, to);
        if (due.isEmpty()) return;

        NumberFormat vnNum = NumberFormat.getNumberInstance(new Locale("vi"));

        for (Object[] row : due) {
            Long pawnId = (Long) row[0];
            String customerName = (String) row[1];
            String phone = (String) row[2];
            LocalDateTime dueDate = (LocalDateTime) row[3];
            BigDecimal amount = (BigDecimal) row[4];

            String amountStr = vnNum.format(amount == null ? BigDecimal.ZERO : amount) + " ₫";
            String dateStr = dueDate.format(DATE_FMT);

            tappyMessageClient.sendPawnDueReminder(phone, customerName, amountStr, dateStr, pawnId);
        }
        log.info("Pawn due borrower reminders queued for tenant {}: {} contract(s)",
                tenant.getTenantId(), due.size());
    }

    /**
     * Finds appointments scheduled in the 60-minute window starting from
     * {@code windowStart}, sends a Zalo reminder (via the Tappy Message service) to each customer
     * (if they have a phone number), then marks {@code reminderSent = true}.
     * TenantContext must be set by the caller before invoking this method.
     */
    @Transactional
    public void sendAppointmentReminders(Tenant tenant, LocalTime windowStart) {
        LocalDate today = LocalDate.now();
        LocalTime windowEnd = windowStart.plusMinutes(60);

        List<Appointment> due = appointmentRepository.findDueForReminder(
                tenant.getTenantId(), today, windowStart, windowEnd);

        if (due.isEmpty()) return;

        for (Appointment appt : due) {
            try {
                if (appt.getCustomerPhone() != null && !appt.getCustomerPhone().isBlank()) {
                    String serviceNames = appt.getServices() == null || appt.getServices().isEmpty() ? "—"
                            : appt.getServices().stream()
                            .map(AppointmentServiceItem::getProductName)
                            .filter(Objects::nonNull)
                            .collect(Collectors.joining(", "));
                    String time = appt.getScheduledStartTime().format(TIME_FMT);
                    String date = appt.getScheduledDate().format(DATE_FMT);

                    tappyMessageClient.sendAppointmentReminder(
                            appt.getCustomerPhone(), appt.getCustomerName(),
                            serviceNames, time, date, appt.getId());
                }
            } finally {
                // Mark sent regardless of whether the phone number exists or ZNS succeeds,
                // so we don't retry the same appointment in a future scheduler tick.
                appt.setReminderSent(true);
            }
        }

        appointmentRepository.saveAll(due);
        log.info("Appointment reminders queued for tenant {}: {} appointment(s)",
                tenant.getTenantId(), due.size());
    }
}
