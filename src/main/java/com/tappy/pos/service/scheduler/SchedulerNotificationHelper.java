package com.tappy.pos.service.scheduler;

import com.tappy.pos.model.entity.notification.Notification;
import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.model.enums.PawnStatus;
import com.tappy.pos.model.enums.RoleEnum;
import com.tappy.pos.repository.order.OrderRepository;
import com.tappy.pos.repository.pawn.PawnRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final NotificationService notificationService;
    private final MessageService messageService;

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

        Locale vi = new Locale("vi");
        String dateStr = today.format(DATE_FMT);
        String revenueStr = NumberFormat.getNumberInstance(new Locale("vi")).format(revenue) + " ₫";

        String title = messageService.getMessage("notification.revenue.daily.title", vi, dateStr);
        String message = messageService.getMessage("notification.revenue.daily.message", vi, count, revenueStr);

        notificationService.pushToRoles(Notification.NotificationType.INFO, title, message,
                "REVENUE", null, List.of(RoleEnum.SHOP_OWNER.getCode()));
        log.info("Daily revenue summary sent for tenant {}: {} orders, {}", tenant.getTenantId(), count, revenue);
    }

    /**
     * Queries PAWNED contracts due today and pushes a notification to SHOP_OWNER and MANAGER.
     * TenantContext must be set by the caller before invoking this method.
     */
    @Transactional
    public void sendPawnDueNotification(Tenant tenant) {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = today.atTime(LocalTime.MAX);

        List<Object[]> result = pawnRepository.sumByPawnStatusAndPawnDueDateBetween(
                PawnStatus.PAWNED, from, to, false);

        if (result.isEmpty()) return;
        long count = ((Number) result.get(0)[1]).longValue();
        if (count == 0) return;

        Locale vi = new Locale("vi");
        String dateStr = today.format(DATE_FMT);
        String title = messageService.getMessage("notification.pawn.due.title", vi, count);
        String message = messageService.getMessage("notification.pawn.due.message", vi, count, dateStr);

        notificationService.pushToRoles(Notification.NotificationType.INFO, title, message,
                "PAWN", null, List.of(RoleEnum.SHOP_OWNER.getCode(), RoleEnum.MANAGER.getCode()));
        log.info("Pawn due notification sent for tenant {}: {} contract(s)", tenant.getTenantId(), count);
    }
}
