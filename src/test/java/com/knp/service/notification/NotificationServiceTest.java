package com.knp.service.notification;

import com.knp.exception.ResourceNotFoundException;
import com.knp.model.dto.notification.CreateNotificationRequest;
import com.knp.model.dto.notification.NotificationDTO;
import com.knp.model.entity.notification.Notification;
import com.knp.model.entity.tenant.Tenant;
import com.knp.repository.auth.UserRepository;
import com.knp.repository.notification.NotificationRepository;
import com.knp.service.MessageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Unit Tests")
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;
    @Mock private MessageService messageService;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("testuser", null, Collections.emptyList()));
        lenient().when(messageService.getMessage(anyString(), any(Object[].class))).thenReturn("msg");
        lenient().when(messageService.getMessage(anyString(), any(), any())).thenReturn("msg");
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

        NotificationDTO dto = notificationService.markRead(1L);

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

        int count = notificationService.markAllRead();

        assertThat(count).isEqualTo(3);
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

    // ── pushExpiryWarning ─────────────────────────────────────────────────────

    @Test
    @DisplayName("pushExpiryWarning: sends billing notifications to shop owners")
    void pushExpiryWarning_success() {
        Tenant tenant = new Tenant();
        tenant.setTenantId("shop1");
        tenant.setName("Shop One");
        tenant.setExpirationDate(LocalDate.now().plusDays(3));

        when(userRepository.findUsernamesByRole("SHOP_OWNER")).thenReturn(List.of("owner1", "owner2"));
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

        when(userRepository.findUsernamesByRole("SHOP_OWNER")).thenReturn(Collections.emptyList());

        notificationService.pushExpiryWarning(tenant, 1);

        verify(notificationRepository, never()).saveAll(any());
    }
}
