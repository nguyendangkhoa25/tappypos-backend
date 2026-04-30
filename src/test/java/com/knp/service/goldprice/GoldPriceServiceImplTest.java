package com.knp.service.goldprice;

import com.knp.exception.BadRequestException;
import com.knp.exception.ResourceNotFoundException;
import com.knp.model.dto.goldprice.GoldPriceDTO;
import com.knp.model.dto.goldprice.PriceBoardResponse;
import com.knp.model.entity.tenant.GoldPrice;
import com.knp.model.entity.tenant.ShopInfo;
import com.knp.model.enums.ShopConfigKey;
import com.knp.repository.tenant.GoldPriceRepository;
import com.knp.repository.tenant.ShopInfoRepository;
import com.knp.service.MessageService;
import com.knp.service.tenant.ShopConfigService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoldPriceServiceImpl Unit Tests")
class GoldPriceServiceImplTest {

    @Mock private GoldPriceRepository goldPriceRepository;
    @Mock private ShopInfoRepository shopInfoRepository;
    @Mock private ShopConfigService shopConfigService;
    @Mock private MessageService messageService;

    @InjectMocks
    private GoldPriceServiceImpl service;

    private GoldPrice goldPrice;
    private ShopInfo shopInfo;

    @BeforeEach
    void setUp() {
        goldPrice = new GoldPrice();
        goldPrice.setId(1L);
        goldPrice.setCode("SJC");
        goldPrice.setLabel("Vàng SJC");
        goldPrice.setBuy(new BigDecimal("8200000"));
        goldPrice.setSell(new BigDecimal("8250000"));
        goldPrice.setDeleted(false);
        goldPrice.setShowInBoard(true);

        shopInfo = new ShopInfo();
        shopInfo.setShopName("Tiệm Vàng ABC");
        shopInfo.setAddress("123 Main St");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("golduser", null, Collections.emptyList()));

        lenient().when(messageService.getMessage(anyString(), any(Object[].class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(messageService.getMessage(anyString()))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(messageService.getMessage(eq("error.goldprice.invalidBoardCode")))
                .thenReturn("Invalid price board code.");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── getAllPrices ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllPrices: returns list of all active prices")
    void getAllPrices() {
        when(goldPriceRepository.findAllActive()).thenReturn(List.of(goldPrice));

        List<GoldPriceDTO> result = service.getAllPrices();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("SJC");
    }

    // ── updatePrice ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("updatePrice: updates fields and records updatedBy")
    void updatePrice_success() {
        when(goldPriceRepository.findById(1L)).thenReturn(Optional.of(goldPrice));
        when(goldPriceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        GoldPriceDTO dto = new GoldPriceDTO();
        dto.setBuy(new BigDecimal("8300000"));
        dto.setSell(new BigDecimal("8350000"));
        dto.setShowInBoard(true);

        GoldPriceDTO result = service.updatePrice(1L, dto);

        assertThat(goldPrice.getBuy()).isEqualByComparingTo(new BigDecimal("8300000"));
        assertThat(goldPrice.getUpdatedBy()).isEqualTo("golduser");
    }

    @Test
    @DisplayName("updatePrice: throws when price not found")
    void updatePrice_notFound() {
        when(goldPriceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updatePrice(99L, new GoldPriceDTO()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updatePrice: throws when price is soft-deleted")
    void updatePrice_deleted() {
        goldPrice.setDeleted(true);
        when(goldPriceRepository.findById(1L)).thenReturn(Optional.of(goldPrice));

        assertThatThrownBy(() -> service.updatePrice(1L, new GoldPriceDTO()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getPriceBoard ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getPriceBoard: returns board with shop info and prices")
    void getPriceBoard_success() {
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(shopInfo));
        when(shopConfigService.getString(ShopConfigKey.PRICE_BOARD_CODE)).thenReturn(null);
        when(goldPriceRepository.findAllVisibleInBoard()).thenReturn(List.of(goldPrice));

        PriceBoardResponse response = service.getPriceBoard("any-code");

        assertThat(response.getShopName()).isEqualTo("Tiệm Vàng ABC");
        assertThat(response.getPrices()).hasSize(1);
    }

    @Test
    @DisplayName("getPriceBoard: throws when shop info not configured")
    void getPriceBoard_noShopInfo() {
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPriceBoard("code"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getPriceBoard: throws when code does not match configured code")
    void getPriceBoard_invalidCode() {
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(shopInfo));
        when(shopConfigService.getString(ShopConfigKey.PRICE_BOARD_CODE)).thenReturn("correct-code");

        assertThatThrownBy(() -> service.getPriceBoard("wrong-code"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid price board code");
    }

    @Test
    @DisplayName("getPriceBoard: accepts any code when no code is configured")
    void getPriceBoard_noConfiguredCode() {
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(shopInfo));
        when(shopConfigService.getString(ShopConfigKey.PRICE_BOARD_CODE)).thenReturn(null);
        when(goldPriceRepository.findAllVisibleInBoard()).thenReturn(List.of());

        PriceBoardResponse response = service.getPriceBoard("anything");

        assertThat(response).isNotNull();
    }
}
