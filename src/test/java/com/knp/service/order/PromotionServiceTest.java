package com.knp.service.order;

import com.knp.exception.BadRequestException;
import com.knp.exception.ResourceNotFoundException;
import com.knp.model.dto.promotion.ApplyPromotionResponse;
import com.knp.model.dto.promotion.SavePromotionRequest;
import com.knp.model.entity.order.Promotion;
import com.knp.model.enums.DiscountType;
import com.knp.repository.order.PromotionRepository;
import com.knp.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PromotionService Unit Tests")
class PromotionServiceTest {

    @Mock private PromotionRepository promotionRepository;
    @Mock private MessageService messageService;

    @InjectMocks
    private PromotionService promotionService;

    private Promotion percentagePromo;
    private Promotion amountPromo;

    @BeforeEach
    void setUp() {
        percentagePromo = Promotion.builder()
                .name("10% OFF")
                .code("SAVE10")
                .type(DiscountType.PERCENTAGE)
                .value(new BigDecimal("10"))
                .usedCount(0)
                .isActive(true)
                .build();

        amountPromo = Promotion.builder()
                .name("50K OFF")
                .code("FLAT50K")
                .type(DiscountType.AMOUNT)
                .value(new BigDecimal("50000"))
                .usedCount(0)
                .isActive(true)
                .build();

        lenient().when(messageService.getMessage(anyString(), any(Object[].class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(messageService.getMessage(anyString()))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(messageService.getMessage(eq("error.promotion.code.exists"), any(Object[].class)))
                .thenReturn("Promotion code already exists.");
    }

    // ── validatePromotion ─────────────────────────────────────────────────────

    @Test
    @DisplayName("validatePromotion: calculates percentage discount correctly")
    void validatePromotion_percentage() {
        when(promotionRepository.findValidPromotion(eq("SAVE10"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(percentagePromo));

        ApplyPromotionResponse response = promotionService.validatePromotion("SAVE10", new BigDecimal("200000"));

        assertThat(response.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("20000.00"));
    }

    @Test
    @DisplayName("validatePromotion: caps percentage discount at maxDiscountAmount")
    void validatePromotion_percentageCappedAtMax() {
        percentagePromo.setMaxDiscountAmount(new BigDecimal("15000"));
        when(promotionRepository.findValidPromotion(eq("SAVE10"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(percentagePromo));

        ApplyPromotionResponse response = promotionService.validatePromotion("SAVE10", new BigDecimal("200000"));

        assertThat(response.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("15000"));
    }

    @Test
    @DisplayName("validatePromotion: calculates flat amount discount")
    void validatePromotion_flatAmount() {
        when(promotionRepository.findValidPromotion(eq("FLAT50K"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(amountPromo));

        ApplyPromotionResponse response = promotionService.validatePromotion("FLAT50K", new BigDecimal("200000"));

        assertThat(response.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("50000.00"));
    }

    @Test
    @DisplayName("validatePromotion: discount cannot exceed order subtotal")
    void validatePromotion_discountCappedAtSubtotal() {
        when(promotionRepository.findValidPromotion(eq("FLAT50K"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(amountPromo));

        ApplyPromotionResponse response = promotionService.validatePromotion("FLAT50K", new BigDecimal("30000"));

        assertThat(response.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("30000"));
    }

    @Test
    @DisplayName("validatePromotion: throws when minimum order amount not met")
    void validatePromotion_belowMinOrder() {
        percentagePromo.setMinOrderAmount(new BigDecimal("100000"));
        when(promotionRepository.findValidPromotion(anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.of(percentagePromo));

        assertThatThrownBy(() -> promotionService.validatePromotion("SAVE10", new BigDecimal("50000")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least");
    }

    @Test
    @DisplayName("validatePromotion: throws for invalid/expired code")
    void validatePromotion_invalidCode() {
        when(promotionRepository.findValidPromotion(anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> promotionService.validatePromotion("INVALID", new BigDecimal("100000")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid or expired");
    }

    // ── applyAtCheckout ───────────────────────────────────────────────────────

    @Test
    @DisplayName("applyAtCheckout: increments usedCount")
    void applyAtCheckout_incrementsUsedCount() {
        when(promotionRepository.findValidPromotion(eq("SAVE10"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(percentagePromo));
        when(promotionRepository.save(any())).thenReturn(percentagePromo);

        promotionService.applyAtCheckout("SAVE10", new BigDecimal("100000"));

        ArgumentCaptor<Promotion> captor = ArgumentCaptor.forClass(Promotion.class);
        verify(promotionRepository).save(captor.capture());
        assertThat(captor.getValue().getUsedCount()).isEqualTo(1);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create: saves new promotion with uppercased code")
    void create_success() {
        SavePromotionRequest req = new SavePromotionRequest();
        req.setName("Test Promo");
        req.setCode("testcode");
        req.setType(DiscountType.AMOUNT);
        req.setValue(new BigDecimal("10000"));

        when(promotionRepository.findByCode("TESTCODE")).thenReturn(Optional.empty());
        when(promotionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        promotionService.create(req);

        ArgumentCaptor<Promotion> cap = ArgumentCaptor.forClass(Promotion.class);
        verify(promotionRepository).save(cap.capture());
        assertThat(cap.getValue().getCode()).isEqualTo("TESTCODE");
    }

    @Test
    @DisplayName("create: throws when code already exists")
    void create_duplicateCode() {
        SavePromotionRequest req = new SavePromotionRequest();
        req.setCode("SAVE10");
        req.setName("Dupe");
        req.setType(DiscountType.AMOUNT);
        req.setValue(BigDecimal.TEN);

        when(promotionRepository.findByCode("SAVE10")).thenReturn(Optional.of(percentagePromo));

        assertThatThrownBy(() -> promotionService.create(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already exists");
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete: soft-deletes existing promotion")
    void delete_success() {
        when(promotionRepository.findById(1L)).thenReturn(Optional.of(percentagePromo));
        when(promotionRepository.save(any())).thenReturn(percentagePromo);

        promotionService.delete(1L);

        verify(promotionRepository).save(any());
        assertThat(percentagePromo.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("delete: throws when promotion not found")
    void delete_notFound() {
        when(promotionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> promotionService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
