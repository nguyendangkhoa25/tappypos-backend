package com.tappy.pos.service.order;

import com.tappy.pos.service.audit.ActivityLogService;

import com.tappy.pos.config.AuthContext;

import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.entity.order.Combo;
import com.tappy.pos.model.entity.order.ComboItem;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.order.ComboRepository;
import com.tappy.pos.repository.order.OrderItemRepository;
import com.tappy.pos.service.MessageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ComboServiceImpl Unit Tests")
class ComboServiceImplTest {

    @Mock private ComboRepository comboRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private TenantContext tenantContext;
    @Mock private MessageService messageService;

    @Mock
    private AuthContext authContext;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private ComboServiceImpl service;

    private Combo combo(Long id, String name, boolean active) {
        Combo c = Combo.builder()
                .name(name)
                .description("desc")
                .price(new BigDecimal("100000"))
                .active(active)
                .build();
        c.setId(id);
        c.getItems().add(ComboItem.builder()
                .combo(c).productId(5L).productName("Sản phẩm X")
                .quantity(2).price(new BigDecimal("60000")).build());
        return c;
    }

    @Test
    @DisplayName("list(active): filters by active and maps to DTO with computed total")
    void list_active() {
        when(comboRepository.findByDeletedFalseAndActive(true)).thenReturn(List.of(combo(1L, "Combo A", true)));

        List<Map<String, Object>> result = service.list(true);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("name")).isEqualTo("Combo A");
        assertThat(result.get(0).get("id")).isEqualTo("1");
        assertThat(result.get(0).get("totalIndividualPrice")).isEqualTo(new BigDecimal("120000")); // 60000 * 2
        assertThat((List<?>) result.get(0).get("items")).hasSize(1);
    }

    @Test
    @DisplayName("list(null): returns all non-deleted combos")
    void list_all() {
        when(comboRepository.findByDeletedFalse()).thenReturn(List.of(combo(1L, "Combo A", true), combo(2L, "Combo B", false)));

        assertThat(service.list(null)).hasSize(2);
    }

    @Test
    @DisplayName("create: builds combo with items and returns DTO")
    void create() {
        when(comboRepository.save(any(Combo.class))).thenAnswer(inv -> {
            Combo c = inv.getArgument(0);
            c.setId(9L);
            return c;
        });

        Map<String, Object> body = Map.of(
                "name", "Combo Mới",
                "price", "150000",
                "active", true,
                "items", List.of(Map.of("productId", "5", "productName", "X", "quantity", 2, "price", "50000")));

        Map<String, Object> dto = service.create(body);

        assertThat(dto.get("id")).isEqualTo("9");
        assertThat(dto.get("name")).isEqualTo("Combo Mới");
        assertThat(dto.get("active")).isEqualTo(true);
        assertThat((List<?>) dto.get("items")).hasSize(1);
        assertThat(dto.get("totalIndividualPrice")).isEqualTo(new BigDecimal("100000")); // 50000 * 2
        verify(comboRepository).save(any(Combo.class));
    }

    @Test
    @DisplayName("update: applies fields and replaces items")
    void update() {
        Combo existing = combo(3L, "Old", true);
        when(comboRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(comboRepository.save(any(Combo.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> body = Map.of(
                "name", "New Name",
                "active", false,
                "items", List.of(Map.of("productId", "7", "productName", "Y", "price", "30000")));

        Map<String, Object> dto = service.update(3L, body);

        assertThat(dto.get("name")).isEqualTo("New Name");
        assertThat(dto.get("active")).isEqualTo(false);
        assertThat((List<?>) dto.get("items")).hasSize(1);
    }

    @Test
    @DisplayName("update: not found → ResourceNotFoundException")
    void update_notFound() {
        when(comboRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, Map.of()))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(comboRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete: soft-deletes the combo")
    void delete() {
        Combo existing = combo(4L, "X", true);
        when(comboRepository.findById(4L)).thenReturn(Optional.of(existing));

        service.delete(4L);

        assertThat(existing.getDeleted()).isTrue();
        verify(comboRepository).save(existing);
    }

    @Test
    @DisplayName("delete: not found → ResourceNotFoundException")
    void delete_notFound() {
        when(comboRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getAnalytics: summary, ranking and default (day) trend")
    void getAnalytics() {
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();
        when(orderItemRepository.getComboSummary(from, to))
                .thenReturn(List.<Object[]>of(new Object[]{10L, 5_000_000.0, null, 250_000.0}));
        when(comboRepository.findByDeletedFalseAndActive(true)).thenReturn(List.of(combo(1L, "A", true)));
        when(orderItemRepository.getComboRanking(eq(from), eq(to), anyInt()))
                .thenReturn(List.<Object[]>of(new Object[]{1L, "Combo A", 8L, 800_000.0, 6L}));
        when(orderItemRepository.getComboTrendByDay(from, to))
                .thenReturn(List.<Object[]>of(new Object[]{"2026-06-01", 3L, 300_000.0}));

        Map<String, Object> result = service.getAnalytics(from, to, "day", 5);

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) result.get("summary");
        assertThat(summary.get("totalSold")).isEqualTo(10L);
        assertThat(summary.get("activeCount")).isEqualTo(1L);
        assertThat((List<?>) result.get("ranking")).hasSize(1);
        assertThat((List<?>) result.get("trend")).hasSize(1);
    }

    @Test
    @DisplayName("getAnalytics: empty summary → zeroed metrics, week trend")
    void getAnalytics_emptyWeek() {
        LocalDateTime from = LocalDateTime.now().minusDays(30);
        LocalDateTime to = LocalDateTime.now();
        when(orderItemRepository.getComboSummary(from, to)).thenReturn(List.of());
        when(comboRepository.findByDeletedFalseAndActive(true)).thenReturn(List.of());
        when(orderItemRepository.getComboRanking(eq(from), eq(to), anyInt())).thenReturn(List.of());
        when(orderItemRepository.getComboTrendByWeek(from, to)).thenReturn(List.of());

        Map<String, Object> result = service.getAnalytics(from, to, "week", 5);

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) result.get("summary");
        assertThat(summary.get("totalSold")).isEqualTo(0L);
        assertThat(summary.get("activeCount")).isEqualTo(0L);
    }
}
