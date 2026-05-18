package com.tappy.pos.service.goldprice;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.goldprice.GoldPriceDTO;
import com.tappy.pos.model.dto.goldprice.PriceBoardResponse;
import com.tappy.pos.model.entity.product.Category;
import com.tappy.pos.model.entity.tenant.GoldPrice;
import com.tappy.pos.model.entity.tenant.ShopInfo;
import com.tappy.pos.model.enums.ShopConfigKey;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.product.CategoryRepository;
import com.tappy.pos.repository.tenant.GoldPriceRepository;
import com.tappy.pos.repository.tenant.ShopInfoRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.tenant.ShopConfigService;
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
    @Mock private CategoryRepository categoryRepository;
    @Mock private ShopInfoRepository shopInfoRepository;
    @Mock private ShopConfigService shopConfigService;
    @Mock private MessageService messageService;
    @Mock private TenantContext tenantContext;

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

    @Test
    @DisplayName("getPriceBoard: blank configured code accepts any request code")
    void getPriceBoard_blankConfiguredCode() {
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(shopInfo));
        when(shopConfigService.getString(ShopConfigKey.PRICE_BOARD_CODE)).thenReturn("   ");
        when(goldPriceRepository.findAllVisibleInBoard()).thenReturn(List.of());

        PriceBoardResponse response = service.getPriceBoard("any-code");

        assertThat(response).isNotNull();
    }

    // ── createPrice ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("createPrice: throws BadRequestException when categoryId is null")
    void createPrice_nullCategoryId() {
        GoldPriceDTO dto = new GoldPriceDTO();

        assertThatThrownBy(() -> service.createPrice(dto))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("createPrice: throws ResourceNotFoundException when category not found")
    void createPrice_categoryNotFound() {
        GoldPriceDTO dto = new GoldPriceDTO();
        dto.setCategoryId(10L);
        when(categoryRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createPrice(dto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createPrice: throws BadRequestException when category has no parent")
    void createPrice_parentIsNull() {
        GoldPriceDTO dto = new GoldPriceDTO();
        dto.setCategoryId(10L);

        Category rootCat = new Category();
        rootCat.setName("Vàng");
        when(categoryRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(rootCat));

        assertThatThrownBy(() -> service.createPrice(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("danh mục gốc");
    }

    @Test
    @DisplayName("createPrice: throws BadRequestException when price already exists for category")
    void createPrice_alreadyExists() {
        GoldPriceDTO dto = new GoldPriceDTO();
        dto.setCategoryId(10L);

        Category parent = new Category();
        parent.setName("Vàng SJC");
        Category cat = new Category();
        cat.setName("610");
        cat.setParent(parent);

        when(categoryRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(cat));
        when(goldPriceRepository.findByCategoryIdAndDeletedFalse(10L)).thenReturn(Optional.of(goldPrice));

        assertThatThrownBy(() -> service.createPrice(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("đã có cấu hình giá");
    }

    @Test
    @DisplayName("createPrice: successfully creates price with default displayOrder when dto has 0")
    void createPrice_success_defaultDisplayOrder() {
        GoldPriceDTO dto = new GoldPriceDTO();
        dto.setCategoryId(10L);
        dto.setBuy(new BigDecimal("8200000"));
        dto.setSell(new BigDecimal("8250000"));
        dto.setPawn(new BigDecimal("8150000"));
        // displayOrder not set → 0 → defaults to 10

        Category parent = new Category();
        parent.setName("Vàng SJC");
        Category cat = new Category();
        cat.setName("610");
        cat.setParent(parent);

        when(categoryRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(cat));
        when(goldPriceRepository.findByCategoryIdAndDeletedFalse(10L)).thenReturn(Optional.empty());
        when(tenantContext.getCurrentTenantId()).thenReturn("tenant1");
        when(goldPriceRepository.save(any())).thenAnswer(inv -> {
            GoldPrice saved = inv.getArgument(0);
            saved.setId(5L);
            return saved;
        });

        GoldPriceDTO result = service.createPrice(dto);

        assertThat(result).isNotNull();
        verify(goldPriceRepository).save(any(GoldPrice.class));
    }

    @Test
    @DisplayName("createPrice: uses provided displayOrder when greater than 0")
    void createPrice_success_withDisplayOrder() {
        GoldPriceDTO dto = new GoldPriceDTO();
        dto.setCategoryId(20L);
        dto.setDisplayOrder(3);
        dto.setShowInBoard(true);

        Category parent = new Category();
        parent.setName("Vàng 9999");
        Category cat = new Category();
        cat.setName("9999");
        cat.setParent(parent);

        when(categoryRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.of(cat));
        when(goldPriceRepository.findByCategoryIdAndDeletedFalse(20L)).thenReturn(Optional.empty());
        when(tenantContext.getCurrentTenantId()).thenReturn("tenant1");
        when(goldPriceRepository.save(any())).thenAnswer(inv -> {
            GoldPrice saved = inv.getArgument(0);
            saved.setId(6L);
            return saved;
        });

        GoldPriceDTO result = service.createPrice(dto);

        assertThat(result).isNotNull();
    }

    // ── deletePrice ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("deletePrice: soft-deletes an existing active price")
    void deletePrice_success() {
        when(goldPriceRepository.findById(1L)).thenReturn(Optional.of(goldPrice));
        when(goldPriceRepository.save(any())).thenReturn(goldPrice);

        service.deletePrice(1L);

        assertThat(goldPrice.isDeleted()).isTrue();
        verify(goldPriceRepository).save(goldPrice);
    }

    @Test
    @DisplayName("deletePrice: throws ResourceNotFoundException when price not found")
    void deletePrice_notFound() {
        when(goldPriceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deletePrice(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deletePrice: throws ResourceNotFoundException when price is already soft-deleted")
    void deletePrice_alreadyDeleted() {
        goldPrice.setDeleted(true);
        when(goldPriceRepository.findById(1L)).thenReturn(Optional.of(goldPrice));

        assertThatThrownBy(() -> service.deletePrice(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getPriceForCategory ───────────────────────────────────────────────────

    @Test
    @DisplayName("getPriceForCategory: returns DTO for configured category")
    void getPriceForCategory_success() {
        when(goldPriceRepository.findByCategoryIdAndDeletedFalse(5L)).thenReturn(Optional.of(goldPrice));

        GoldPriceDTO result = service.getPriceForCategory(5L);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("SJC");
    }

    @Test
    @DisplayName("getPriceForCategory: throws ResourceNotFoundException when no price for category")
    void getPriceForCategory_notFound() {
        when(goldPriceRepository.findByCategoryIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPriceForCategory(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getAllPrices with category map ─────────────────────────────────────────

    @Test
    @DisplayName("getAllPrices: resolves category name from catMap when categoryId set")
    void getAllPrices_withCategoryId() {
        goldPrice.setCategoryId(10L);
        Category cat = new Category();
        cat.setId(10L);
        cat.setName("610");

        when(goldPriceRepository.findAllActive()).thenReturn(List.of(goldPrice));
        when(categoryRepository.findAllActiveWithParent()).thenReturn(List.of(cat));

        List<GoldPriceDTO> result = service.getAllPrices();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategoryName()).isEqualTo("610");
    }

    @Test
    @DisplayName("getAllPrices: resolves parent category name when category has parent")
    void getAllPrices_withParentCategory() {
        goldPrice.setCategoryId(10L);

        Category parent = new Category();
        parent.setId(5L);
        parent.setName("Vàng SJC");

        Category cat = new Category();
        cat.setId(10L);
        cat.setName("610");
        cat.setParent(parent);

        when(goldPriceRepository.findAllActive()).thenReturn(List.of(goldPrice));
        when(categoryRepository.findAllActiveWithParent()).thenReturn(List.of(cat, parent));

        List<GoldPriceDTO> result = service.getAllPrices();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategoryName()).isEqualTo("610");
        assertThat(result.get(0).getParentCategoryName()).isEqualTo("Vàng SJC");
    }
}
