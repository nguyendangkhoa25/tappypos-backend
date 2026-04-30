package com.knp.service.notification;

import com.knp.exception.ResourceNotFoundException;
import com.knp.service.MessageService;
import com.knp.model.dto.notification.CreateNotificationRequest;
import com.knp.model.dto.notification.NotificationDTO;
import com.knp.model.entity.notification.Notification;
import com.knp.model.entity.tenant.Tenant;
import com.knp.model.enums.RoleEnum;
import com.knp.repository.notification.NotificationRepository;
import com.knp.repository.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final MessageService messageService;

    public Page<NotificationDTO> getForCurrentUser(Pageable pageable, Notification.NotificationType type) {
        String username = currentUsername();
        Page<Notification> page = (type != null)
                ? notificationRepository.findByUserIdAndType(username, type, pageable)
                : notificationRepository.findByUserId(username, pageable);
        return page.map(this::mapToDTO);
    }

    public long getUnreadCount() {
        return notificationRepository.countUnread(currentUsername());
    }

    @Transactional
    public NotificationDTO markRead(Long id) {
        String username = currentUsername();
        Notification n = notificationRepository.findById(id)
                .filter(x -> !x.isDeleted() && x.getUserId().equals(username))
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.notification.not.found", id)));
        if (!Boolean.TRUE.equals(n.getIsRead())) {
            n.markRead();
            notificationRepository.save(n);
        }
        return mapToDTO(n);
    }

    @Transactional
    public int markAllRead() {
        return notificationRepository.markAllRead(currentUsername());
    }

    @Transactional
    public void delete(Long id) {
        String username = currentUsername();
        Notification n = notificationRepository.findById(id)
                .filter(x -> !x.isDeleted() && x.getUserId().equals(username))
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.notification.not.found", id)));
        n.softDelete();
        notificationRepository.save(n);
    }

    /**
     * Create notifications.
     * If targetUserIds is empty/null, broadcasts to all active users in this tenant.
     */
    @Transactional
    public List<NotificationDTO> create(CreateNotificationRequest req) {
        String creator = currentUsername();
        Notification.NotificationType type = parseType(req.getType());

        List<String> targets = (req.getTargetUserIds() != null && !req.getTargetUserIds().isEmpty())
                ? req.getTargetUserIds()
                : userRepository.findAllActiveUsernames();

        List<Notification> created = targets.stream().map(userId -> Notification.builder()
                .userId(userId)
                .title(req.getTitle().trim())
                .message(req.getMessage())
                .type(type)
                .referenceType(req.getReferenceType())
                .referenceId(req.getReferenceId())
                .isRead(false)
                .createdBy(creator)
                .build()
        ).collect(Collectors.toList());

        List<Notification> saved = notificationRepository.saveAll(created);
        log.info("Created {} notification(s): type={}, title={}", saved.size(), type, req.getTitle());
        return saved.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    /**
     * Called by the scheduler (with TenantContext already set to the target tenant).
     * Finds all SHOP_OWNER users and sends them a BILLING expiry-warning notification.
     *
     * @param daysRemaining days until expiry (1, 3, or 7)
     */
    @Transactional
    public void pushExpiryWarning(Tenant tenant, int daysRemaining) {
        List<String> shopOwners = userRepository.findUsernamesByRole(RoleEnum.SHOP_OWNER.getCode());
        if (shopOwners.isEmpty()) {
            log.warn("No shop owners found for tenant {} — skipping expiry notification", tenant.getTenantId());
            return;
        }

        Locale vi = new Locale("vi");
        String expiryDate = tenant.getExpirationDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String title = messageService.getMessage("notification.expiry.warning.title", vi, daysRemaining);
        String message = messageService.getMessage("notification.expiry.warning.message", vi,
                tenant.getName(), expiryDate);

        List<Notification> notifications = shopOwners.stream().map(username -> Notification.builder()
                .userId(username)
                .title(title)
                .message(message)
                .type(Notification.NotificationType.BILLING)
                .referenceType("TENANT")
                .referenceId(tenant.getId())
                .isRead(false)
                .createdBy("SYSTEM")
                .build()
        ).collect(Collectors.toList());

        notificationRepository.saveAll(notifications);
        log.info("Pushed expiry warning to {} shop owner(s) for tenant {} (expires {})",
                notifications.size(), tenant.getTenantId(), tenant.getExpirationDate());
    }

    /**
     * Broadcast a SYSTEM notification to all active MASTER_TENANT users.
     */
    @Transactional
    public void pushToMasterUsers(String title, String message, String referenceType, Long referenceId) {
        List<String> masterUsers = userRepository.findUsernamesByRole("MASTER_TENANT");
        if (masterUsers.isEmpty()) {
            log.warn("pushToMasterUsers: no MASTER_TENANT users found, skipping notification");
            return;
        }
        List<Notification> notifications = masterUsers.stream().map(userId -> Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .type(Notification.NotificationType.SYSTEM)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .isRead(false)
                .createdBy("SYSTEM")
                .build()
        ).collect(Collectors.toList());
        notificationRepository.saveAll(notifications);
        log.info("Pushed notification to {} master user(s): {}", notifications.size(), title);
    }

    /**
     * Internal API — used by other services to push system notifications.
     */
    @Transactional
    public void pushSystem(String userId, String title, String message,
                           String referenceType, Long referenceId) {
        Notification n = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .type(Notification.NotificationType.SYSTEM)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .isRead(false)
                .createdBy("SYSTEM")
                .build();
        notificationRepository.save(n);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private Notification.NotificationType parseType(String raw) {
        if (raw == null) return Notification.NotificationType.INFO;
        try {
            return Notification.NotificationType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Notification.NotificationType.INFO;
        }
    }

    private NotificationDTO mapToDTO(Notification n) {
        return NotificationDTO.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType().name())
                .referenceType(n.getReferenceType())
                .referenceId(n.getReferenceId())
                .isRead(n.getIsRead())
                .readAt(n.getReadAt())
                .createdBy(n.getCreatedBy())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
