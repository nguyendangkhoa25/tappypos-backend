package com.tappy.pos.service.notification;

import com.tappy.pos.config.FeatureContext;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.model.dto.notification.CreateNotificationRequest;
import com.tappy.pos.model.dto.notification.NotificationDTO;
import com.tappy.pos.model.entity.notification.Notification;
import com.tappy.pos.model.entity.notification.NotificationPreference;
import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.model.enums.RoleEnum;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.notification.NotificationPreferenceRepository;
import com.tappy.pos.repository.notification.NotificationRepository;
import com.tappy.pos.repository.auth.UserRepository;
import com.tappy.pos.repository.tenant.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    private final TenantRepository tenantRepository;
    private final TenantContext tenantContext;
    private final MessageService messageService;
    private final FeatureContext featureContext;

    /**
     * Maps notification types that require a specific feature to receive by default.
     * Types not in this map are always delivered to users who have no preference row.
     */
    private static final Map<Notification.NotificationType, String> TYPE_TO_REQUIRED_FEATURE;
    static {
        Map<Notification.NotificationType, String> m = new EnumMap<>(Notification.NotificationType.class);
        m.put(Notification.NotificationType.ORDER,     "ORDER");
        m.put(Notification.NotificationType.LOW_STOCK, "INVENTORY");
        TYPE_TO_REQUIRED_FEATURE = Collections.unmodifiableMap(m);
    }

    public Page<NotificationDTO> getForCurrentUser(Pageable pageable, Notification.NotificationType type) {
        String username = currentUsername();
        Page<Notification> page = (type != null)
                ? notificationRepository.findByUserIdAndType(username, type.name(), pageable)
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
                .orElseGet(this::defaultTypesForCurrentUser);
    }

    /**
     * Computes feature-based default notification types for a user with no saved preferences.
     * Always includes the base types; adds feature-gated types only when the JWT grants that feature.
     */
    private List<String> defaultTypesForCurrentUser() {
        List<String> defaults = new ArrayList<>(
                Arrays.asList("SYSTEM", "ANNOUNCEMENT", "INFO", "MARKETING", "BILLING"));
        TYPE_TO_REQUIRED_FEATURE.forEach((type, feature) -> {
            if (featureContext.hasFeature(feature)) {
                defaults.add(type.name());
            }
        });
        return defaults;
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

        String tenantId = tenantContext.getCurrentTenantId();
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
                        .tenantId(tenantId)
                        .build())
                .collect(Collectors.toList());

        List<Notification> saved = notificationRepository.saveAll(created);
        log.info("Created {} notification(s): type={}, title={} ({} opted out)",
                saved.size(), type, req.getTitle(), optedOut.size());
        return saved.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    // ── Core push API ─────────────────────────────────────────────────────────

    /**
     * Push a notification to all active users holding any of the given roles.
     * TenantContext must be set by the caller when targeting shop-level roles.
     * Respects per-user opt-out preferences.
     */
    @Transactional
    public void pushToRoles(Notification.NotificationType type, String title, String message,
                             String referenceType, Long referenceId, List<String> roleNames) {
        List<String> targets = userRepository.findUsernamesByRoleNames(roleNames);
        if (targets.isEmpty()) {
            log.warn("pushToRoles: no active users found for roles {}", roleNames);
            return;
        }
        String tenantId = tenantContext.getCurrentTenantId();
        Set<String> optedOut = optedOutUsers(targets, type);
        List<Notification> notifications = targets.stream()
                .filter(userId -> !optedOut.contains(userId))
                .map(userId -> Notification.builder()
                        .userId(userId)
                        .title(title)
                        .message(message)
                        .type(type)
                        .referenceType(referenceType)
                        .referenceId(referenceId)
                        .isRead(false)
                        .createdBy("SYSTEM")
                        .tenantId(tenantId)
                        .build())
                .collect(Collectors.toList());
        notificationRepository.saveAll(notifications);
        log.info("pushToRoles: {} notification(s) → roles={} ({} opted out)",
                notifications.size(), roleNames, optedOut.size());
    }

    /**
     * Async fire-and-forget variant of pushToRoles.
     * Pass tenantId when targeting shop-level roles so TenantContext is set on the async thread.
     * Pass null for master-context notifications (MASTER_TENANT, AGENT).
     *
     * NOT_SUPPORTED overrides the class-level readOnly=true so the async thread starts
     * no outer transaction; each repository call inside pushToRoles starts its own
     * writable (or read-only) transaction after TenantContext has already been set.
     */
    @Async
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void pushToRolesAsync(Notification.NotificationType type, String title, String message,
                                  String referenceType, Long referenceId,
                                  List<String> roleNames, String tenantId) {
        try {
            if (tenantId != null) {
                tenantRepository.findByTenantId(tenantId).ifPresent(tenantContext::setCurrentTenant);
            }
            pushToRoles(type, title, message, referenceType, referenceId, roleNames);
        } catch (Exception e) {
            log.warn("Async pushToRoles failed (roles={}, tenantId={}): {}", roleNames, tenantId, e.getMessage(), e);
        } finally {
            if (tenantId != null) {
                tenantContext.clear();
            }
        }
    }

    // ── Specialised push methods (delegate to pushToRoles) ────────────────────

    /**
     * Called by the scheduler (TenantContext already set by caller).
     * Sends a BILLING expiry-warning notification to all SHOP_OWNER users.
     */
    @Transactional
    public void pushExpiryWarning(Tenant tenant, int daysRemaining) {
        Locale vi = new Locale("vi");
        String expiryDate = tenant.getExpirationDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String title = messageService.getMessage("notification.expiry.warning.title", vi, daysRemaining);
        String message = messageService.getMessage("notification.expiry.warning.message", vi,
                tenant.getName(), expiryDate);
        pushToRoles(Notification.NotificationType.BILLING, title, message,
                "TENANT", tenant.getId(), List.of(RoleEnum.SHOP_OWNER.getCode()));
    }

    /**
     * Broadcast a SYSTEM notification to all active MASTER_TENANT users.
     */
    @Transactional
    public void pushToMasterUsers(String title, String message, String referenceType, Long referenceId) {
        pushToRoles(Notification.NotificationType.SYSTEM, title, message,
                referenceType, referenceId, List.of("MASTER_TENANT"));
    }

    /**
     * Async fire-and-forget variant of pushToMasterUsers.
     */
    @Async
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void pushToMasterUsersAsync(String title, String message, String referenceType, Long referenceId) {
        try {
            pushToRoles(Notification.NotificationType.SYSTEM, title, message,
                    referenceType, referenceId, List.of("MASTER_TENANT"));
        } catch (Exception e) {
            log.warn("Async pushToMasterUsers failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Push a notification to a single user. Respects the user's preference row and
     * feature-based defaults — skips the insert if the user has opted out of this type.
     */
    @Transactional
    public void pushSystem(String userId, Notification.NotificationType type, String title, String message,
                           String referenceType, Long referenceId) {
        Set<String> optedOut = optedOutUsers(List.of(userId), type);
        if (optedOut.contains(userId)) {
            log.debug("pushSystem: skipped — user {} opted out of type {}", userId, type);
            return;
        }
        Notification n = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .type(type)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .isRead(false)
                .createdBy("SYSTEM")
                .tenantId(tenantContext.getCurrentTenantId())
                .build();
        notificationRepository.save(n);
    }

    /**
     * Async fire-and-forget variant of pushSystem targeting a single known user.
     * Pass tenantId when the user belongs to a shop tenant; null for master context.
     */
    @Async
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void pushSystemAsync(String userId, Notification.NotificationType type,
                                String title, String message,
                                String referenceType, Long referenceId, String tenantId) {
        try {
            if (tenantId != null) {
                tenantRepository.findByTenantId(tenantId).ifPresent(tenantContext::setCurrentTenant);
            }
            pushSystem(userId, type, title, message, referenceType, referenceId);
        } catch (Exception e) {
            log.warn("Async pushSystem failed (userId={}, tenantId={}): {}", userId, tenantId, e.getMessage(), e);
        } finally {
            if (tenantId != null) {
                tenantContext.clear();
            }
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns user IDs from the given list who should NOT receive the specified type.
     * Two sources of opt-out:
     *   1. Explicit pref row — type is not in the user's enabled list.
     *   2. No pref row + feature-gated type — user's roles don't grant the required feature.
     */
    private Set<String> optedOutUsers(List<String> userIds, Notification.NotificationType type) {
        List<NotificationPreference> prefs = preferenceRepository.findByUserIdIn(userIds);
        Map<String, NotificationPreference> prefMap = prefs.stream()
                .collect(Collectors.toMap(NotificationPreference::getUserId, p -> p));

        Set<String> optedOut = new HashSet<>();

        // Explicit opt-out: pref row exists but type not in enabled list
        prefs.stream()
                .filter(p -> !"ALL".equals(p.getEnabledTypes())
                        && !Arrays.asList(p.getEnabledTypes().split(",")).contains(type.name()))
                .map(NotificationPreference::getUserId)
                .forEach(optedOut::add);

        // Default opt-out: no pref row + type requires a feature the user's roles don't grant
        String requiredFeature = TYPE_TO_REQUIRED_FEATURE.get(type);
        if (requiredFeature != null) {
            List<String> usersWithoutPref = userIds.stream()
                    .filter(id -> !prefMap.containsKey(id))
                    .collect(Collectors.toList());
            if (!usersWithoutPref.isEmpty()) {
                Set<String> usersWithFeature = userRepository.findUsernamesWithFeature(usersWithoutPref, requiredFeature);
                usersWithoutPref.stream()
                        .filter(id -> !usersWithFeature.contains(id))
                        .forEach(optedOut::add);
            }
        }

        return optedOut;
    }

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
