package com.tappy.pos.service.finance;

import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.marketprice.MarketPriceDTO;
import com.tappy.pos.model.dto.marketprice.SaveMarketPriceRequest;
import com.tappy.pos.model.entity.finance.MarketPrice;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.finance.MarketPriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarketPriceService Unit Tests")
class MarketPriceServiceTest {

    @Mock private MarketPriceRepository repository;
    @Mock private TenantContext tenantContext;

    @InjectMocks private MarketPriceService marketPriceService;

    private MarketPrice goldPrice;

    @BeforeEach
    void setUp() {
        goldPrice = MarketPrice.builder()
                .name("Vàng SJC")
                .unit("Lượng")
                .buyPrice(new BigDecimal("8200000"))
                .sellPrice(new BigDecimal("8250000"))
                .isActive(true)
                .notes("Giá hôm nay")
                .sortOrder(1)
                .build();
        goldPrice.setId(1L);
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAll: returns all active prices as DTOs")
    void getAll_returnsList() {
        when(repository.findAllActive()).thenReturn(List.of(goldPrice));

        List<MarketPriceDTO> result = marketPriceService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Vàng SJC");
        assertThat(result.get(0).getBuyPrice()).isEqualByComparingTo("8200000");
    }

    @Test
    @DisplayName("getAll: returns empty list when no active prices")
    void getAll_emptyList() {
        when(repository.findAllActive()).thenReturn(List.of());

        List<MarketPriceDTO> result = marketPriceService.getAll();

        assertThat(result).isEmpty();
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create: saves market price with all fields")
    void create_success() {
        SaveMarketPriceRequest req = SaveMarketPriceRequest.builder()
                .name("Bạc")
                .unit("Chỉ")
                .buyPrice(new BigDecimal("800000"))
                .sellPrice(new BigDecimal("850000"))
                .isActive(true)
                .notes("Giá bạc")
                .sortOrder(2)
                .build();

        when(tenantContext.getCurrentTenantId()).thenReturn("tenant1");
        when(repository.save(any())).thenAnswer(inv -> {
            MarketPrice saved = inv.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        MarketPriceDTO result = marketPriceService.create(req);

        assertThat(result.getName()).isEqualTo("Bạc");
        assertThat(result.getBuyPrice()).isEqualByComparingTo("800000");
        assertThat(result.getSortOrder()).isEqualTo(2);
        verify(repository).save(any(MarketPrice.class));
    }

    @Test
    @DisplayName("create: defaults isActive to true when not provided")
    void create_defaultIsActive() {
        SaveMarketPriceRequest req = SaveMarketPriceRequest.builder()
                .name("Vàng 9999")
                .unit("Lượng")
                .buyPrice(new BigDecimal("8100000"))
                .isActive(null)
                .sortOrder(null)
                .build();

        when(tenantContext.getCurrentTenantId()).thenReturn("tenant1");
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MarketPriceDTO result = marketPriceService.create(req);

        assertThat(result.getIsActive()).isTrue();
        assertThat(result.getSortOrder()).isEqualTo(999);
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update: patches existing price fields")
    void update_success() {
        when(repository.findById(1L)).thenReturn(Optional.of(goldPrice));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SaveMarketPriceRequest req = SaveMarketPriceRequest.builder()
                .name("Vàng SJC Updated")
                .unit("Lượng")
                .buyPrice(new BigDecimal("8300000"))
                .sellPrice(new BigDecimal("8350000"))
                .isActive(false)
                .notes("Updated note")
                .sortOrder(3)
                .build();

        MarketPriceDTO result = marketPriceService.update(1L, req);

        assertThat(result.getName()).isEqualTo("Vàng SJC Updated");
        assertThat(result.getBuyPrice()).isEqualByComparingTo("8300000");
        assertThat(result.getIsActive()).isFalse();
        assertThat(result.getSortOrder()).isEqualTo(3);
    }

    @Test
    @DisplayName("update: skips isActive update when null in request")
    void update_nullIsActive_keepsExisting() {
        when(repository.findById(1L)).thenReturn(Optional.of(goldPrice));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SaveMarketPriceRequest req = SaveMarketPriceRequest.builder()
                .name("Vàng SJC")
                .unit("Lượng")
                .buyPrice(new BigDecimal("8200000"))
                .isActive(null)
                .sortOrder(null)
                .build();

        MarketPriceDTO result = marketPriceService.update(1L, req);

        assertThat(result.getIsActive()).isTrue(); // unchanged
    }

    @Test
    @DisplayName("update: throws ResourceNotFoundException when price not found")
    void update_notFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> marketPriceService.update(99L, new SaveMarketPriceRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("update: throws ResourceNotFoundException when price is soft-deleted")
    void update_softDeleted() {
        goldPrice.setDeleted(true);
        when(repository.findById(1L)).thenReturn(Optional.of(goldPrice));

        assertThatThrownBy(() -> marketPriceService.update(1L, new SaveMarketPriceRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete: soft-deletes existing active price")
    void delete_success() {
        when(repository.findById(1L)).thenReturn(Optional.of(goldPrice));
        when(repository.save(any())).thenReturn(goldPrice);

        marketPriceService.delete(1L);

        assertThat(goldPrice.isDeleted()).isTrue();
        verify(repository).save(goldPrice);
    }

    @Test
    @DisplayName("delete: throws ResourceNotFoundException when not found")
    void delete_notFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> marketPriceService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("delete: throws ResourceNotFoundException when already deleted")
    void delete_alreadyDeleted() {
        goldPrice.setDeleted(true);
        when(repository.findById(1L)).thenReturn(Optional.of(goldPrice));

        assertThatThrownBy(() -> marketPriceService.delete(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
