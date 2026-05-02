package com.knp.service.notification;

import com.knp.exception.ResourceNotFoundException;
import com.knp.service.MessageService;
import com.knp.model.dto.notification.CreateNotificationRequest;
import com.knp.model.dto.notification.NotificationDTO;
import com.knp.model.entity.notification.Notification;
import com.knp.model.entity.notification.NotificationPreference;
import com.knp.model.entity.tenant.Tenant;
import com.knp.model.enums.RoleEnum;
import com.knp.repository.notification.NotificationPreferenceRepository;
import com.knp.repository.notification.NotificationRepository;
import com.knp.repository.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
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

    // ── Notification preferences ──────────────────────────────────────────────

    public List<String> getPreferences() {
        return preferenceRepository.findByUserId(currentUsername())
                .map(p -> "ALL".equals(p.getEnabledTypes())
                        ? List.of("ALL")
                        : Arrays.asList(p.getEnabledTypes().split(",")))
                .orElse(List.of("ALL"));
    }

    @Transactional
    public List<String> savePreferences(List<String> enabledTypes) {
        String username = currentUsername();
        NotificationPreference pref = preferenceRepository.findByUserId(username)
                .orElse(NotificationPreference.builder().userId(username).build());
        boolean allEnabled = enabledTypes == null || enabledTypes.isEmpty()
                || enabledTypes.contains("ALL");
        pref.setEnabledTypes(allEnabled ? "ALL" : String.join(",", enabledTypes));
        preferenceRepository.save(pref);
        log.info("Saved notification preferences for {}: {}", username, pref.getEnabledTypes());
        return getPreferences();
    }

    /**
     * Returns user IDs from the given list who have opted out of the specified type.
     * Single batch query — avoids N+1.
     */
    private Set<String> optedOutUsers(List<String> userIds, Notification.NotificationType type) {
        return preferenceRepository.findByUserIdIn(userIds).stream()
                .filter(p -> !"ALL".equals(p.getEnabledTypes())
                        && !Arrays.asList(p.getEnabledTypes().split(",")).contains(type.name()))
                .map(NotificationPreference::getUserId)
                .collect(Collectors.toSet());
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

        Set<String> optedOut = optedOutUsers(targets, type);
        List<Notification> created = targets.stream()
                .filter(userId -> !optedOut.contains(userId))
                .map(userId -> Notification.builder()
                        .userId(userId)
                        .title(req.getTitle().trim())
                        .message(req.getMessage())
                        .type(type)
                        .referenceType(req.getReferenceType())
                        .referenceId(req.getReferenceId())
                        .isRead(false)
                        .createdBy(creator)
                        .build())
                .collect(Collectors.toList());

        List<Notification> saved = notificationRepository.saveAll(created);
        log.info("Created {} notification(s): type={}, title={} ({} opted out)",
                saved.size(), type, req.getTitle(), optedOut.size());
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

        Set<String> optedOut = optedOutUsers(shopOwners, Notification.NotificationType.BILLING);
        List<Notification> notifications = shopOwners.stream()
                .filter(username -> !optedOut.contains(username))
                .map(username -> Notification.builder()
                        .userId(username)
                        .title(title)
                        .message(message)
                        .type(Notification.NotificationType.BILLING)
                        .referenceType("TENANT")
                        .referenceId(tenant.getId())
                        .isRead(false)
                        .createdBy("SYSTEM")
                        .build())
                .collect(Collectors.toList());

        notificationRepository.saveAll(notifications);
        log.info("Pushed expiry warning to {} shop owner(s) for tenant {} (expires {}, {} opted out)",
                notifications.size(), tenant.getTenantId(), tenant.getExpirationDate(), optedOut.size());
    }

    /**
     * Async fire-and-forget variant — caller is never blocked or thrown at.
     */
    @Async
    public void pushToMasterUsersAsync(String title, String message, String referenceType, Long referenceId) {
        try {
            pushToMasterUsers(title, message, referenceType, referenceId);
        } catch (Exception e) {
            log.warn("Async pushToMasterUsers failed: {}", e.getMessage());
        }
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
        Set<String> optedOut = optedOutUsers(masterUsers, Notification.NotificationType.SYSTEM);
        List<Notification> notifications = masterUsers.stream()
                .filter(userId -> !optedOut.contains(userId))
                .map(userId -> Notification.builder()
                        .userId(userId)
                        .title(title)
                        .message(message)
                        .type(Notification.NotificationType.SYSTEM)
                        .referenceType(referenceType)
                        .referenceId(referenceId)
                        .isRead(false)
                        .createdBy("SYSTEM")
                        .build())
                .collect(Collectors.toList());
        notificationRepository.saveAll(notifications);
        log.info("Pushed notification to {} master user(s): {} ({} opted out)",
                notifications.size(), title, optedOut.size());
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
