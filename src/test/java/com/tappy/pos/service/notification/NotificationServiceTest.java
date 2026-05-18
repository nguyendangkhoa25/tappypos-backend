package com.tappy.pos.service.notification;

import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.notification.CreateNotificationRequest;
import com.tappy.pos.model.dto.notification.NotificationDTO;
import com.tappy.pos.model.entity.notification.Notification;
import com.tappy.pos.model.entity.notification.NotificationPreference;
import com.tappy.pos.model.entity.tenant.Tenant;
import com.tappy.pos.repository.auth.UserRepository;
import com.tappy.pos.repository.notification.NotificationPreferenceRepository;
import com.tappy.pos.repository.notification.NotificationRepository;
import com.tappy.pos.service.MessageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Unit Tests")
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationPreferenceRepository preferenceRepository;
    @Mock private UserRepository userRepository;
    @Mock private com.tappy.pos.repository.tenant.TenantRepository tenantRepository;
    @Mock private com.tappy.pos.multitenant.TenantContext tenantContext;
    @Mock private MessageService messageService;
    @Mock private com.tappy.pos.config.FeatureContext featureContext;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("testuser", null, Collections.emptyList()));
        lenient().when(messageService.getMessage(anyString(), any(Object[].class))).thenReturn("msg");
        lenient().when(messageService.getMessage(anyString(), any(), any())).thenReturn("msg");
        // Default: no users have opted out of anything
        lenient().when(preferenceRepository.findByUserIdIn(anyList())).thenReturn(Collections.emptyList());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── getUnreadCount ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getUnreadCount: delegates to repository with current username")
    void getUnreadCount() {
        when(notificationRepository.countUnread("testuser")).thenReturn(5L);

        assertThat(notificationService.getUnreadCount()).isEqualTo(5L);
    }

    // ── markRead ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("markRead: marks notification as read")
    void markRead_success() {
        Notification n = Notification.builder()
                .userId("testuser").title("Test").isRead(false)
                .type(Notification.NotificationType.INFO)
                .createdBy("SYSTEM")
                .build();
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));
        when(notificationRepository.save(any())).thenReturn(n);

        notificationService.markRead(1L);

        assertThat(n.getIsRead()).isTrue();
        verify(notificationRepository).save(n);
    }

    @Test
    @DisplayName("markRead: skips save if already read")
    void markRead_alreadyRead() {
        Notification n = Notification.builder()
                .userId("testuser").title("Test").isRead(true)
                .type(Notification.NotificationType.INFO)
                .createdBy("SYSTEM")
                .build();
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));

        notificationService.markRead(1L);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("markRead: throws when notification not found")
    void markRead_notFound() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markRead(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("markRead: throws when notification belongs to another user")
    void markRead_wrongUser() {
        Notification n = Notification.builder()
                .userId("otheruser").title("Test").isRead(false)
                .type(Notification.NotificationType.INFO)
                .createdBy("SYSTEM")
                .build();
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> notificationService.markRead(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── markAllRead ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("markAllRead: delegates to repository bulk update")
    void markAllRead() {
        when(notificationRepository.markAllRead("testuser")).thenReturn(3);

        assertThat(notificationService.markAllRead()).isEqualTo(3);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete: soft-deletes notification owned by current user")
    void delete_success() {
        Notification n = Notification.builder()
                .userId("testuser").title("Test").isRead(false)
                .type(Notification.NotificationType.INFO)
                .createdBy("SYSTEM")
                .build();
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));
        when(notificationRepository.save(any())).thenReturn(n);

        notificationService.delete(1L);

        assertThat(n.isDeleted()).isTrue();
    }

    // ── delete edge cases ─────────────────────────────────────────────────────

    @Test
    @DisplayName("delete: throws when notification not found")
    void delete_notFound_throws() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());
        when(messageService.getMessage(anyString(), eq(99L))).thenReturn("not found");

        assertThatThrownBy(() -> notificationService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("delete: throws when notification belongs to different user")
    void delete_wrongUser_throws() {
        Notification n = Notification.builder()
                .userId("other-user").title("Test").isRead(false)
                .type(Notification.NotificationType.INFO)
                .createdBy("SYSTEM").build();
        when(notificationRepository.findById(2L)).thenReturn(Optional.of(n));
        when(messageService.getMessage(anyString(), eq(2L))).thenReturn("not found");

        assertThatThrownBy(() -> notificationService.delete(2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── pushSystem edge cases ─────────────────────────────────────────────────

    @Test
    @DisplayName("pushSystem: skips save when user opted out of the type")
    void pushSystem_userOptedOut_skipsNotification() {
        NotificationPreference pref = NotificationPreference.builder()
                .userId("user1").enabledTypes("BILLING,ORDER").build();
        when(preferenceRepository.findByUserIdIn(List.of("user1"))).thenReturn(List.of(pref));

        notificationService.pushSystem("user1", Notification.NotificationType.SYSTEM,
                "Title", "Body", null, null);

        verify(notificationRepository, never()).save(any());
    }

    // ── pushToRolesAsync ──────────────────────────────────────────────────────

    @Test
    @DisplayName("pushToRolesAsync: sets tenant context when tenantId provided")
    void pushToRolesAsync_withTenantId_setsTenantContext() {
        Tenant tenant = new Tenant();
        tenant.setTenantId("shop-abc");
        when(tenantRepository.findByTenantId("shop-abc")).thenReturn(Optional.of(tenant));
        when(userRepository.findUsernamesByRoleNames(anyList())).thenReturn(Collections.emptyList());

        notificationService.pushToRolesAsync(Notification.NotificationType.INFO, "T", "M",
                null, null, List.of("SHOP_OWNER"), "shop-abc");

        verify(tenantContext).setCurrentTenant(tenant);
        verify(tenantContext).clear();
    }

    @Test
    @DisplayName("pushToRolesAsync: skips context setup when tenantId is null")
    void pushToRolesAsync_withNullTenantId_noContextSetup() {
        when(userRepository.findUsernamesByRoleNames(anyList())).thenReturn(Collections.emptyList());

        notificationService.pushToRolesAsync(Notification.NotificationType.INFO, "T", "M",
                null, null, List.of("MASTER_TENANT"), null);

        verify(tenantRepository, never()).findByTenantId(anyString());
        verify(tenantContext, never()).clear();
    }

    @Test
    @DisplayName("pushToRolesAsync: swallows exception and clears context")
    void pushToRolesAsync_exceptionSwallowed() {
        when(tenantRepository.findByTenantId("shop-err")).thenReturn(Optional.empty());
        when(userRepository.findUsernamesByRoleNames(anyList())).thenThrow(new RuntimeException("DB error"));

        notificationService.pushToRolesAsync(Notification.NotificationType.INFO, "T", "M",
                null, null, List.of("SHOP_OWNER"), "shop-err");

        verify(tenantContext).clear();
    }

    // ── parseType via create ──────────────────────────────────────────────────

    @Test
    @DisplayName("create: null type defaults to INFO")
    void create_withNullType_defaultsToInfo() {
        CreateNotificationRequest req = new CreateNotificationRequest();
        req.setTitle("Test");
        req.setMessage("Body");
        req.setType(null);
        req.setTargetUserIds(List.of("user1"));

        when(notificationRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        List<NotificationDTO> result = notificationService.create(req);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo("INFO");
    }

    @Test
    @DisplayName("create: invalid type string defaults to INFO")
    void create_withInvalidType_defaultsToInfo() {
        CreateNotificationRequest req = new CreateNotificationRequest();
        req.setTitle("Test");
        req.setMessage("Body");
        req.setType("INVALID_TYPE_XYZ");
        req.setTargetUserIds(List.of("user1"));

        when(notificationRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        List<NotificationDTO> result = notificationService.create(req);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo("INFO");
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create: creates notifications for specific target users")
    void create_targetedUsers() {
        CreateNotificationRequest req = new CreateNotificationRequest();
        req.setTitle("Test");
        req.setMessage("Body");
        req.setType("INFO");
        req.setTargetUserIds(List.of("user1", "user2"));

        when(notificationRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        List<NotificationDTO> result = notificationService.create(req);

        assertThat(result).hasSize(2);
        verify(userRepository, never()).findAllActiveUsernames();
    }

    @Test
    @DisplayName("create: broadcasts to all active users when no targets specified")
    void create_broadcast() {
        CreateNotificationRequest req = new CreateNotificationRequest();
        req.setTitle("Broadcast");
        req.setMessage("To all");
        req.setType("SYSTEM");

        when(userRepository.findAllActiveUsernames()).thenReturn(List.of("u1", "u2", "u3"));
        when(notificationRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        List<NotificationDTO> result = notificationService.create(req);

        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("create: excludes users who opted out of the notification type")
    void create_excludesOptedOutUsers() {
        CreateNotificationRequest req = new CreateNotificationRequest();
        req.setTitle("Order update");
        req.setType("ORDER");
        req.setTargetUserIds(List.of("user1", "user2", "user3"));

        // user2 has opted out of ORDER
        NotificationPreference pref = NotificationPreference.builder()
                .userId("user2").enabledTypes("SYSTEM,BILLING").build();
        when(preferenceRepository.findByUserIdIn(List.of("user1", "user2", "user3")))
                .thenReturn(List.of(pref));
        // user1 and user3 have no pref row; stub feature check so they are not filtered out
        when(userRepository.findUsernamesWithFeature(anyList(), eq("ORDER")))
                .thenReturn(Set.of("user1", "user3"));
        when(notificationRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        List<NotificationDTO> result = notificationService.create(req);

        assertThat(result).hasSize(2);
        assertThat(result).noneMatch(n -> n.getUserId().equals("user2"));
    }

    @Test
    @DisplayName("create: includes user who has ALL preference")
    void create_allPreferenceReceivesEverything() {
        CreateNotificationRequest req = new CreateNotificationRequest();
        req.setTitle("Marketing msg");
        req.setType("MARKETING");
        req.setTargetUserIds(List.of("user1"));

        NotificationPreference pref = NotificationPreference.builder()
                .userId("user1").enabledTypes("ALL").build();
        when(preferenceRepository.findByUserIdIn(List.of("user1"))).thenReturn(List.of(pref));
        when(notificationRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        List<NotificationDTO> result = notificationService.create(req);

        assertThat(result).hasSize(1);
    }

    // ── pushExpiryWarning ─────────────────────────────────────────────────────

    @Test
    @DisplayName("pushExpiryWarning: sends billing notifications to shop owners")
    void pushExpiryWarning_success() {
        Tenant tenant = new Tenant();
        tenant.setTenantId("shop1");
        tenant.setName("Shop One");
        tenant.setExpirationDate(LocalDate.now().plusDays(3));

        when(userRepository.findUsernamesByRoleNames(List.of("SHOP_OWNER"))).thenReturn(List.of("owner1", "owner2"));
        when(notificationRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        notificationService.pushExpiryWarning(tenant, 3);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Notification>> cap = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(cap.capture());
        assertThat(cap.getValue()).hasSize(2);
        assertThat(cap.getValue().get(0).getType()).isEqualTo(Notification.NotificationType.BILLING);
    }

    @Test
    @DisplayName("pushExpiryWarning: skips when no shop owners found")
    void pushExpiryWarning_noOwners() {
        Tenant tenant = new Tenant();
        tenant.setTenantId("shop1");
        tenant.setExpirationDate(LocalDate.now().plusDays(1));

        when(userRepository.findUsernamesByRoleNames(List.of("SHOP_OWNER"))).thenReturn(Collections.emptyList());

        notificationService.pushExpiryWarning(tenant, 1);

        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("pushExpiryWarning: excludes owner who opted out of BILLING")
    void pushExpiryWarning_respectsPreference() {
        Tenant tenant = new Tenant();
        tenant.setTenantId("shop1");
        tenant.setName("Shop One");
        tenant.setExpirationDate(LocalDate.now().plusDays(7));

        when(userRepository.findUsernamesByRoleNames(List.of("SHOP_OWNER"))).thenReturn(List.of("owner1", "owner2"));
        NotificationPreference pref = NotificationPreference.builder()
                .userId("owner2").enabledTypes("ORDER,SYSTEM").build();
        when(preferenceRepository.findByUserIdIn(List.of("owner1", "owner2"))).thenReturn(List.of(pref));
        when(notificationRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        notificationService.pushExpiryWarning(tenant, 7);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Notification>> cap = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(cap.capture());
        assertThat(cap.getValue()).hasSize(1);
        assertThat(cap.getValue().get(0).getUserId()).isEqualTo("owner1");
    }

    // ── pushToMasterUsersAsync ────────────────────────────────────────────────

    @Test
    @DisplayName("pushToMasterUsersAsync: sends notification and catches exceptions silently")
    void pushToMasterUsersAsync_callsDelegate() {
        when(userRepository.findUsernamesByRoleNames(List.of("MASTER_TENANT"))).thenReturn(List.of("admin1"));
        when(notificationRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        notificationService.pushToMasterUsersAsync("T", "M", "TENANT", 1L);

        verify(notificationRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("pushToMasterUsersAsync: swallows exceptions without propagating")
    void pushToMasterUsersAsync_swallowsException() {
        when(userRepository.findUsernamesByRoleNames(anyList())).thenThrow(new RuntimeException("db error"));

        // must not throw
        notificationService.pushToMasterUsersAsync("T", "M", "TENANT", 1L);
    }

    // ── pushSystemAsync ───────────────────────────────────────────────────────

    @Test
    @DisplayName("pushSystemAsync: sets tenant context and sends notification when tenantId provided")
    void pushSystemAsync_withTenantId() {
        Tenant tenant = new Tenant();
        tenant.setTenantId("shop1");
        when(tenantRepository.findByTenantId("shop1")).thenReturn(Optional.of(tenant));
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        notificationService.pushSystemAsync("user1", Notification.NotificationType.SYSTEM,
                "Title", "Body", "ORDER", 1L, "shop1");

        verify(tenantContext).setCurrentTenant(tenant);
        verify(tenantContext).clear();
        verify(notificationRepository).save(any());
    }

    @Test
    @DisplayName("pushSystemAsync: skips tenant context when tenantId is null")
    void pushSystemAsync_nullTenantId() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        notificationService.pushSystemAsync("user1", Notification.NotificationType.INFO,
                "T", "M", null, null, null);

        verify(tenantRepository, never()).findByTenantId(anyString());
        verify(tenantContext, never()).clear();
        verify(notificationRepository).save(any());
    }

    @Test
    @DisplayName("pushSystemAsync: swallows exception without propagating")
    void pushSystemAsync_swallowsException() {
        when(tenantRepository.findByTenantId("shop1")).thenThrow(new RuntimeException("db error"));

        notificationService.pushSystemAsync("user1", Notification.NotificationType.SYSTEM,
                "T", "M", null, null, "shop1");

        verify(tenantContext).clear();
    }

    // ── getForCurrentUser ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getForCurrentUser: returns paged notifications without type filter")
    void getForCurrentUser_noTypeFilter() {
        Notification n = Notification.builder().userId("testuser").title("Hello")
                .type(Notification.NotificationType.INFO).isRead(false).createdBy("SYSTEM").build();
        when(notificationRepository.findByUserId(eq("testuser"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(n)));

        Page<NotificationDTO> result = notificationService.getForCurrentUser(Pageable.unpaged(), null);

        assertThat(result.getContent()).hasSize(1);
        verify(notificationRepository, never()).findByUserIdAndType(any(), any(), any());
    }

    @Test
    @DisplayName("getForCurrentUser: filters by type when provided")
    void getForCurrentUser_withTypeFilter() {
        Notification n = Notification.builder().userId("testuser").title("Order")
                .type(Notification.NotificationType.ORDER).isRead(false).createdBy("SYSTEM").build();
        when(notificationRepository.findByUserIdAndType(eq("testuser"), eq("ORDER"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(n)));

        Page<NotificationDTO> result = notificationService.getForCurrentUser(Pageable.unpaged(), Notification.NotificationType.ORDER);

        assertThat(result.getContent()).hasSize(1);
        verify(notificationRepository, never()).findByUserId(any(), any());
    }

    // ── pushToRoles ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("pushToRoles: saves notifications for all matching role users")
    void pushToRoles_success() {
        when(userRepository.findUsernamesByRoleNames(List.of("SHOP_OWNER"))).thenReturn(List.of("owner1", "owner2"));
        when(notificationRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        notificationService.pushToRoles(Notification.NotificationType.INFO, "Title", "Msg",
                "ORDER", 1L, List.of("SHOP_OWNER"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Notification>> cap = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(cap.capture());
        assertThat(cap.getValue()).hasSize(2);
        assertThat(cap.getValue().get(0).getType()).isEqualTo(Notification.NotificationType.INFO);
    }

    @Test
    @DisplayName("pushToRoles: skips save when no users hold the role")
    void pushToRoles_noUsers() {
        when(userRepository.findUsernamesByRoleNames(List.of("CASHIER"))).thenReturn(Collections.emptyList());

        notificationService.pushToRoles(Notification.NotificationType.INFO, "T", "M",
                null, null, List.of("CASHIER"));

        verify(notificationRepository, never()).saveAll(any());
    }

    // ── pushSystem ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("pushSystem: saves single notification for the given userId")
    void pushSystem_success() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        notificationService.pushSystem("user1", Notification.NotificationType.SYSTEM,
                "Title", "Body", "ORDER", 42L);

        ArgumentCaptor<Notification> cap = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(cap.capture());
        assertThat(cap.getValue().getUserId()).isEqualTo("user1");
        assertThat(cap.getValue().getReferenceId()).isEqualTo(42L);
        assertThat(cap.getValue().getCreatedBy()).isEqualTo("SYSTEM");
    }

    // ── pushToMasterUsers ─────────────────────────────────────────────────────

    @Test
    @DisplayName("pushToMasterUsers: broadcasts SYSTEM notification to all master users")
    void pushToMasterUsers_success() {
        when(userRepository.findUsernamesByRoleNames(List.of("MASTER_TENANT"))).thenReturn(List.of("admin1", "admin2"));
        when(notificationRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        notificationService.pushToMasterUsers("Title", "Msg", "TENANT", 1L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Notification>> cap = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(cap.capture());
        assertThat(cap.getValue()).hasSize(2);
        assertThat(cap.getValue().get(0).getType()).isEqualTo(Notification.NotificationType.SYSTEM);
    }

    @Test
    @DisplayName("pushToMasterUsers: skips when no master users found")
    void pushToMasterUsers_noUsers() {
        when(userRepository.findUsernamesByRoleNames(List.of("MASTER_TENANT"))).thenReturn(Collections.emptyList());

        notificationService.pushToMasterUsers("Title", "Msg", "TENANT", 1L);

        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("pushToMasterUsers: excludes master user who opted out of SYSTEM")
    void pushToMasterUsers_respectsPreference() {
        when(userRepository.findUsernamesByRoleNames(List.of("MASTER_TENANT"))).thenReturn(List.of("admin1", "admin2"));
        NotificationPreference pref = NotificationPreference.builder()
                .userId("admin2").enabledTypes("BILLING,ORDER").build();
        when(preferenceRepository.findByUserIdIn(List.of("admin1", "admin2"))).thenReturn(List.of(pref));
        when(notificationRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        notificationService.pushToMasterUsers("Title", "Msg", "TENANT", 1L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Notification>> cap = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(cap.capture());
        assertThat(cap.getValue()).hasSize(1);
        assertThat(cap.getValue().get(0).getUserId()).isEqualTo("admin1");
    }

    // ── getPreferences ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getPreferences")
    class GetPreferences {

        @Test
        @DisplayName("returns default types when no preference row exists")
        void noRow_returnsAll() {
            when(preferenceRepository.findByUserId("testuser")).thenReturn(Optional.empty());

            List<String> result = notificationService.getPreferences();
            assertThat(result).containsAll(List.of("SYSTEM", "ANNOUNCEMENT", "INFO", "MARKETING", "BILLING"));
        }

        @Test
        @DisplayName("returns ALL when preference is set to ALL")
        void prefAll_returnsAll() {
            NotificationPreference pref = NotificationPreference.builder()
                    .userId("testuser").enabledTypes("ALL").build();
            when(preferenceRepository.findByUserId("testuser")).thenReturn(Optional.of(pref));

            assertThat(notificationService.getPreferences()).containsExactly("ALL");
        }

        @Test
        @DisplayName("returns parsed type list when preference has specific types")
        void specificTypes_returnsParsed() {
            NotificationPreference pref = NotificationPreference.builder()
                    .userId("testuser").enabledTypes("ORDER,LOW_STOCK").build();
            when(preferenceRepository.findByUserId("testuser")).thenReturn(Optional.of(pref));

            assertThat(notificationService.getPreferences()).containsExactlyInAnyOrder("ORDER", "LOW_STOCK");
        }
    }

    // ── savePreferences ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("savePreferences")
    class SavePreferences {

        @Test
        @DisplayName("creates new preference row for first-time save")
        void newRow() {
            when(preferenceRepository.findByUserId("testuser")).thenReturn(Optional.empty());
            when(preferenceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            notificationService.savePreferences(List.of("ORDER", "BILLING"));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<NotificationPreference> cap = ArgumentCaptor.forClass(NotificationPreference.class);
            verify(preferenceRepository).save(cap.capture());
            assertThat(cap.getValue().getEnabledTypes()).contains("ORDER");
            assertThat(cap.getValue().getEnabledTypes()).contains("BILLING");
        }

        @Test
        @DisplayName("updates existing preference row")
        void updatesExisting() {
            NotificationPreference existing = NotificationPreference.builder()
                    .userId("testuser").enabledTypes("ORDER").build();
            when(preferenceRepository.findByUserId("testuser")).thenReturn(Optional.of(existing));
            when(preferenceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            notificationService.savePreferences(List.of("BILLING", "SYSTEM"));

            assertThat(existing.getEnabledTypes()).isEqualTo("BILLING,SYSTEM");
        }

        @Test
        @DisplayName("saves ALL when empty list provided")
        void emptyList_savesAll() {
            when(preferenceRepository.findByUserId("testuser")).thenReturn(Optional.empty());
            when(preferenceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            notificationService.savePreferences(Collections.emptyList());

            @SuppressWarnings("unchecked")
            ArgumentCaptor<NotificationPreference> cap = ArgumentCaptor.forClass(NotificationPreference.class);
            verify(preferenceRepository).save(cap.capture());
            assertThat(cap.getValue().getEnabledTypes()).isEqualTo("ALL");
        }

        @Test
        @DisplayName("saves ALL when list contains ALL")
        void allInList_savesAll() {
            when(preferenceRepository.findByUserId("testuser")).thenReturn(Optional.empty());
            when(preferenceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            notificationService.savePreferences(List.of("ALL"));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<NotificationPreference> cap = ArgumentCaptor.forClass(NotificationPreference.class);
            verify(preferenceRepository).save(cap.capture());
            assertThat(cap.getValue().getEnabledTypes()).isEqualTo("ALL");
        }
    }
}
