package com.knp.service;

import com.knp.exception.ResourceNotFoundException;
import com.knp.model.dto.marketprice.MarketPriceDTO;
import com.knp.model.dto.marketprice.SaveMarketPriceRequest;
import com.knp.model.entity.MarketPrice;
import com.knp.repository.MarketPriceRepository;
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

    @Mock
    private MarketPriceRepository repository;

    @InjectMocks
    private MarketPriceService marketPriceService;

    private MarketPrice gold24k;
    private SaveMarketPriceRequest saveRequest;

    @BeforeEach
    void setUp() {
        gold24k = MarketPrice.builder()
                .name("Vàng 24K")
                .unit("chỉ")
                .buyPrice(new BigDecimal("7500000"))
                .sellPrice(new BigDecimal("7600000"))
                .isActive(true)
                .notes("Giá vàng SJC")
                .sortOrder(1)
                .build();
        gold24k.setId(1L);

        saveRequest = new SaveMarketPriceRequest();
        saveRequest.setName("Vàng 24K");
        saveRequest.setUnit("chỉ");
        saveRequest.setBuyPrice(new BigDecimal("7500000"));
        saveRequest.setSellPrice(new BigDecimal("7600000"));
        saveRequest.setIsActive(true);
        saveRequest.setNotes("Giá vàng SJC");
        saveRequest.setSortOrder(1);
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return all active market prices")
    void testGetAll_ReturnsActiveList() {
        when(repository.findAllActive()).thenReturn(List.of(gold24k));

        List<MarketPriceDTO> result = marketPriceService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Vàng 24K");
        verify(repository).findAllActive();
    }

    @Test
    @DisplayName("Should return empty list when no active market prices")
    void testGetAll_Empty() {
        when(repository.findAllActive()).thenReturn(List.of());

        List<MarketPriceDTO> result = marketPriceService.getAll();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should map all fields to DTO correctly")
    void testGetAll_MapsFieldsCorrectly() {
        when(repository.findAllActive()).thenReturn(List.of(gold24k));

        MarketPriceDTO dto = marketPriceService.getAll().get(0);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Vàng 24K");
        assertThat(dto.getUnit()).isEqualTo("chỉ");
        assertThat(dto.getBuyPrice()).isEqualByComparingTo("7500000");
        assertThat(dto.getSellPrice()).isEqualByComparingTo("7600000");
        assertThat(dto.getIsActive()).isTrue();
        assertThat(dto.getNotes()).isEqualTo("Giá vàng SJC");
        assertThat(dto.getSortOrder()).isEqualTo(1);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should create market price with all fields")
    void testCreate_Success() {
        when(repository.save(any(MarketPrice.class))).thenReturn(gold24k);

        MarketPriceDTO result = marketPriceService.create(saveRequest);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Vàng 24K");
        verify(repository).save(any(MarketPrice.class));
    }

    @Test
    @DisplayName("Should default isActive to true when not provided")
    void testCreate_DefaultsIsActiveTrue() {
        saveRequest.setIsActive(null);
        when(repository.save(any(MarketPrice.class))).thenReturn(gold24k);

        marketPriceService.create(saveRequest);

        verify(repository).save(argThat(mp -> mp.getIsActive()));
    }

    @Test
    @DisplayName("Should default sortOrder to 999 when not provided")
    void testCreate_DefaultsSortOrder() {
        saveRequest.setSortOrder(null);
        when(repository.save(any(MarketPrice.class))).thenReturn(gold24k);

        marketPriceService.create(saveRequest);

        verify(repository).save(argThat(mp -> mp.getSortOrder() == 999));
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should update market price fields")
    void testUpdate_Success() {
        when(repository.findById(1L)).thenReturn(Optional.of(gold24k));
        when(repository.save(any(MarketPrice.class))).thenReturn(gold24k);

        SaveMarketPriceRequest updateReq = new SaveMarketPriceRequest();
        updateReq.setName("Vàng 18K");
        updateReq.setUnit("chỉ");
        updateReq.setBuyPrice(new BigDecimal("5000000"));
        updateReq.setSellPrice(new BigDecimal("5100000"));
        updateReq.setIsActive(true);
        updateReq.setSortOrder(2);

        MarketPriceDTO result = marketPriceService.update(1L, updateReq);

        assertThat(result).isNotNull();
        verify(repository).findById(1L);
        verify(repository).save(any(MarketPrice.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent price")
    void testUpdate_NotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> marketPriceService.update(99L, saveRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating soft-deleted price")
    void testUpdate_SoftDeleted() {
        gold24k.setDeleted(true);
        when(repository.findById(1L)).thenReturn(Optional.of(gold24k));

        assertThatThrownBy(() -> marketPriceService.update(1L, saveRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should soft-delete an active market price")
    void testDelete_Success() {
        when(repository.findById(1L)).thenReturn(Optional.of(gold24k));
        when(repository.save(any(MarketPrice.class))).thenReturn(gold24k);

        marketPriceService.delete(1L);

        verify(repository).findById(1L);
        verify(repository).save(any(MarketPrice.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent price")
    void testDelete_NotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> marketPriceService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
