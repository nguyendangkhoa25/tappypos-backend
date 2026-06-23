package com.tappy.pos.service.tenant;

import com.tappy.pos.model.dto.pawn.PawnSetting;
import com.tappy.pos.model.dto.tenant.MobileShopInfoDTO;
import com.tappy.pos.model.dto.tenant.PosConfigDTO;
import com.tappy.pos.model.dto.tenant.PublicShopInfoDTO;
import com.tappy.pos.model.dto.tenant.ShopInfoDTO;
import com.tappy.pos.model.dto.tenant.UpdateMobileShopConfigRequest;
import com.tappy.pos.model.entity.tenant.ShopInfo;
import com.tappy.pos.model.enums.ShopConfigKey;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.tenant.ShopInfoRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.storage.R2CleanupService;
import com.tappy.pos.service.storage.R2StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShopInfoService Unit Tests")
class ShopInfoServiceTest {

    @Mock
    private ShopInfoRepository shopInfoRepository;

    @Mock
    private ShopConfigService shopConfigService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private R2StorageService r2StorageService;

    @Mock
    private R2CleanupService r2CleanupService;

    @Mock
    private TenantContext tenantContext;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private ShopInfoService shopInfoService;

    private ShopInfo testShopInfo;
    private ShopInfoDTO testShopInfoDTO;

    @BeforeEach
    void setUp() {
        testShopInfo = ShopInfo.builder()
                .shopName("Test Shop")
                .address("123 Main St")
                .companyName("Test Company")
                .phone("0123456789")
                .email("shop@example.com")
                .supplierTaxCode("0123456789")
                .website("https://example.com")
                .build();
        testShopInfo.setId(1L);
        testShopInfo.setCreatedAt(LocalDateTime.now());
        testShopInfo.setUpdatedAt(LocalDateTime.now());

        testShopInfoDTO = ShopInfoDTO.builder()
                .id(1L)
                .shopName("Test Shop")
                .address("123 Main St")
                .companyName("Test Company")
                .defaultTaxRate(10.0)
                .phone("0123456789")
                .email("shop@example.com")
                .supplierTaxCode("0123456789")
                .website("https://example.com")
                .invoiceVendor("VIETTEL")
                .templateCode("TEMPLATE_001")
                .invoiceSeries("001")
                .invoiceSystem("VAT")
                .eInvoiceUsername("username")
                .build();
    }

    // ============= getShopInfo Tests =============

    @Test
    @DisplayName("Should get existing shop info successfully")
    void testGetShopInfo_Success() {
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(testShopInfo));

        ShopInfoDTO result = shopInfoService.getShopInfo();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getShopName()).isEqualTo("Test Shop");
        assertThat(result.getAddress()).isEqualTo("123 Main St");
        verify(shopInfoRepository).findFirstByDeletedAtIsNullOrderByIdAsc();
    }

    @Test
    @DisplayName("Should create shop info if not exists")
    void testGetShopInfo_CreateNew() {
        ShopInfo newShopInfo = new ShopInfo();
        newShopInfo.setId(1L);
        newShopInfo.setCreatedAt(LocalDateTime.now());

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.empty());
        when(shopInfoRepository.save(any(ShopInfo.class))).thenReturn(newShopInfo);

        ShopInfoDTO result = shopInfoService.getShopInfo();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(shopInfoRepository).findFirstByDeletedAtIsNullOrderByIdAsc();
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    // ============= updateShopInfo Tests =============

    @Test
    @DisplayName("Should update shop info with all fields")
    void testUpdateShopInfo_AllFields_Success() {
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class))).thenReturn(testShopInfo);
        when(shopConfigService.getDouble(eq(ShopConfigKey.DEFAULT_TAX_RATE), anyDouble())).thenReturn(10.0);

        ShopInfoDTO result = shopInfoService.updateShopInfo(testShopInfoDTO);

        assertThat(result).isNotNull();
        assertThat(result.getShopName()).isEqualTo("Test Shop");
        assertThat(result.getDefaultTaxRate()).isEqualTo(10.0);
        verify(shopInfoRepository).findFirstByDeletedAtIsNullOrderByIdAsc();
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should update only shop name")
    void testUpdateShopInfo_OnlyShopName() {
        ShopInfoDTO updateDTO = ShopInfoDTO.builder().shopName("New Shop Name").build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class))).thenReturn(testShopInfo);

        shopInfoService.updateShopInfo(updateDTO);

        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should update only address")
    void testUpdateShopInfo_OnlyAddress() {
        ShopInfoDTO updateDTO = ShopInfoDTO.builder().address("456 New Address").build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class))).thenReturn(testShopInfo);

        shopInfoService.updateShopInfo(updateDTO);

        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should update tax rate via ShopConfigService")
    void testUpdateShopInfo_TaxRate() {
        ShopInfoDTO updateDTO = ShopInfoDTO.builder().defaultTaxRate(15.0).build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class))).thenReturn(testShopInfo);

        shopInfoService.updateShopInfo(updateDTO);

        verify(shopConfigService).set(ShopConfigKey.DEFAULT_TAX_RATE, 15.0);
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should update e-invoice username via ShopConfigService")
    void testUpdateShopInfo_EInvoiceCredentials() {
        ShopInfoDTO updateDTO = ShopInfoDTO.builder().eInvoiceUsername("newuser").build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class))).thenReturn(testShopInfo);

        shopInfoService.updateShopInfo(updateDTO);

        verify(shopConfigService).set(ShopConfigKey.EINVOICE_USERNAME, "newuser");
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should update invoice info via ShopConfigService")
    void testUpdateShopInfo_InvoiceInfo() {
        ShopInfoDTO updateDTO = ShopInfoDTO.builder()
                .invoiceVendor("VIETTEL")
                .templateCode("TEMPLATE_002")
                .invoiceSeries("002")
                .build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class))).thenReturn(testShopInfo);

        shopInfoService.updateShopInfo(updateDTO);

        verify(shopConfigService).set(ShopConfigKey.INVOICE_VENDOR, "VIETTEL");
        verify(shopConfigService).set(ShopConfigKey.EINVOICE_TEMPLATE_CODE, "TEMPLATE_002");
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should create shop info if not exists during update")
    void testUpdateShopInfo_CreateNew() {
        ShopInfo newShopInfo = new ShopInfo();
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.empty());
        when(shopInfoRepository.save(any(ShopInfo.class))).thenReturn(newShopInfo);

        shopInfoService.updateShopInfo(testShopInfoDTO);

        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should skip update if identity fields are null")
    void testUpdateShopInfo_NullFields() {
        ShopInfoDTO emptyDTO = ShopInfoDTO.builder().build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class))).thenReturn(testShopInfo);

        shopInfoService.updateShopInfo(emptyDTO);

        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should skip empty string shop name update")
    void testUpdateShopInfo_EmptyShopName() {
        ShopInfoDTO updateDTO = ShopInfoDTO.builder().shopName("   ").build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class))).thenReturn(testShopInfo);

        shopInfoService.updateShopInfo(updateDTO);

        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    // ============= getDefaultTaxRate Tests =============

    @Test
    @DisplayName("Should get default tax rate from ShopConfigService")
    void testGetDefaultTaxRate_Success() {
        when(shopConfigService.getDouble(eq(ShopConfigKey.DEFAULT_TAX_RATE), anyDouble())).thenReturn(10.0);

        Double result = shopInfoService.getDefaultTaxRate();

        assertThat(result).isEqualTo(10.0);
        verify(shopConfigService).getDouble(eq(ShopConfigKey.DEFAULT_TAX_RATE), anyDouble());
    }

    @Test
    @DisplayName("Should return 0.0 when tax rate config not set")
    void testGetDefaultTaxRate_NotFound() {
        when(shopConfigService.getDouble(eq(ShopConfigKey.DEFAULT_TAX_RATE), anyDouble())).thenReturn(0.0);

        Double result = shopInfoService.getDefaultTaxRate();

        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should return default 0.0 when tax rate is null in config")
    void testGetDefaultTaxRate_NullValue() {
        when(shopConfigService.getDouble(eq(ShopConfigKey.DEFAULT_TAX_RATE), anyDouble())).thenReturn(0.0);

        Double result = shopInfoService.getDefaultTaxRate();

        assertThat(result).isEqualTo(0.0);
    }

    // ============= getPublicShopInfo Tests =============

    @Test
    @DisplayName("Should get public shop info without credentials")
    void testGetPublicShopInfo_Success() {
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(testShopInfo));

        PublicShopInfoDTO result = shopInfoService.getPublicShopInfo();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getShopName()).isEqualTo("Test Shop");
        assertThat(result.getPhone()).isEqualTo("0123456789");
        verify(shopInfoRepository).findFirstByDeletedAtIsNullOrderByIdAsc();
    }

    @Test
    @DisplayName("Should create default public shop info if not exists")
    void testGetPublicShopInfo_CreateNew() {
        ShopInfo newShopInfo = new ShopInfo();
        newShopInfo.setId(1L);
        newShopInfo.setCreatedAt(LocalDateTime.now());

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.empty());
        when(shopInfoRepository.save(any(ShopInfo.class))).thenReturn(newShopInfo);

        PublicShopInfoDTO result = shopInfoService.getPublicShopInfo();

        assertThat(result).isNotNull();
        verify(shopInfoRepository).findFirstByDeletedAtIsNullOrderByIdAsc();
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    // ============= Edge Case Tests =============

    @Test
    @DisplayName("Should handle all optional fields as null")
    void testUpdateShopInfo_AllOptionalNull() {
        ShopInfoDTO updateDTO = ShopInfoDTO.builder()
                .shopName(null).address(null).companyName(null).defaultTaxRate(null).phone(null)
                .build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class))).thenReturn(testShopInfo);

        shopInfoService.updateShopInfo(updateDTO);

        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should handle special characters in shop name")
    void testUpdateShopInfo_SpecialCharacters() {
        ShopInfoDTO updateDTO = ShopInfoDTO.builder().shopName("Shop <>&\"'").build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class))).thenReturn(testShopInfo);

        shopInfoService.updateShopInfo(updateDTO);

        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should handle unicode characters in address")
    void testUpdateShopInfo_UnicodeAddress() {
        ShopInfoDTO updateDTO = ShopInfoDTO.builder()
                .address("123 Đường Nguyễn Hữu Cảnh, Quận 1, Thành phố Hồ Chí Minh")
                .build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class))).thenReturn(testShopInfo);

        shopInfoService.updateShopInfo(updateDTO);

        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should update multiple fields together")
    void testUpdateShopInfo_MultipleFields() {
        ShopInfoDTO updateDTO = ShopInfoDTO.builder()
                .shopName("Updated Shop").address("New Address").companyName("New Company")
                .defaultTaxRate(20.0).phone("0987654321")
                .build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class))).thenReturn(testShopInfo);

        shopInfoService.updateShopInfo(updateDTO);

        verify(shopInfoRepository, times(1)).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should handle updating only address field")
    void testUpdateShopInfo_AddressOnly() {
        ShopInfoDTO updateDTO = ShopInfoDTO.builder().address("456 Park Street").build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class))).thenReturn(testShopInfo);

        shopInfoService.updateShopInfo(updateDTO);

        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should handle null tax rate (no config write)")
    void testUpdateShopInfo_NullTaxRate() {
        ShopInfoDTO updateDTO = ShopInfoDTO.builder().defaultTaxRate(null).build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class))).thenReturn(testShopInfo);

        shopInfoService.updateShopInfo(updateDTO);

        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should update website field")
    void testUpdateShopInfo_Website() {
        ShopInfoDTO updateDTO = ShopInfoDTO.builder().website("https://newsite.com").build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class))).thenReturn(testShopInfo);

        shopInfoService.updateShopInfo(updateDTO);

        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should update template code via config service")
    void testUpdateShopInfo_TemplateCode() {
        ShopInfoDTO updateDTO = ShopInfoDTO.builder().templateCode("NEW_TEMPLATE_001").build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class))).thenReturn(testShopInfo);

        shopInfoService.updateShopInfo(updateDTO);

        verify(shopConfigService).set(ShopConfigKey.EINVOICE_TEMPLATE_CODE, "NEW_TEMPLATE_001");
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should update invoice series via config service")
    void testUpdateShopInfo_InvoiceSeries() {
        ShopInfoDTO updateDTO = ShopInfoDTO.builder().invoiceSeries("002").build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class))).thenReturn(testShopInfo);

        shopInfoService.updateShopInfo(updateDTO);

        verify(shopConfigService).set(ShopConfigKey.EINVOICE_SERIES, "002");
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should handle very long shop name")
    void testUpdateShopInfo_LongShopName() {
        String longName = "A".repeat(500);
        ShopInfoDTO updateDTO = ShopInfoDTO.builder().shopName(longName).build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class))).thenReturn(testShopInfo);

        shopInfoService.updateShopInfo(updateDTO);

        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should get shop info when none exists - creates default")
    void testGetShopInfo_CreateDefault() {
        ShopInfo newShopInfo = new ShopInfo();
        newShopInfo.setId(2L);
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.empty());
        when(shopInfoRepository.save(any(ShopInfo.class))).thenReturn(newShopInfo);

        ShopInfoDTO result = shopInfoService.getShopInfo();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2L);
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    // ============= getExcludeVisibleItemFlag Tests =============

    @Test
    @DisplayName("getExcludeVisibleItemFlag: returns true when config flag is true")
    void testGetExcludeVisibleItemFlag_True() {
        when(shopConfigService.getBoolean(ShopConfigKey.PAWN_EXCLUDE_VISIBLE_ITEM, false)).thenReturn(true);

        assertThat(shopInfoService.getExcludeVisibleItemFlag()).isTrue();
    }

    @Test
    @DisplayName("getExcludeVisibleItemFlag: returns false when config flag is false")
    void testGetExcludeVisibleItemFlag_False() {
        when(shopConfigService.getBoolean(ShopConfigKey.PAWN_EXCLUDE_VISIBLE_ITEM, false)).thenReturn(false);

        assertThat(shopInfoService.getExcludeVisibleItemFlag()).isFalse();
    }

    // ============= updatePawnSetting Tests =============

    @Test
    @DisplayName("updatePawnSetting: writes all fields when interestRate non-null and type/dueDate > 0")
    void testUpdatePawnSetting_AllFieldsSet() {
        PawnSetting setting = PawnSetting.builder()
                .interestRate(new java.math.BigDecimal("3.5"))
                .interestType(30)
                .dueDate(60)
                .build();

        PawnSetting result = shopInfoService.updatePawnSetting(setting);

        verify(shopConfigService).set(ShopConfigKey.PAWN_INTEREST_RATE, new java.math.BigDecimal("3.5"));
        verify(shopConfigService).set(ShopConfigKey.PAWN_INTEREST_TYPE, 30);
        verify(shopConfigService).set(ShopConfigKey.PAWN_DUE_DATE, 60);
        assertThat(result).isSameAs(setting);
    }

    @Test
    @DisplayName("updatePawnSetting: skips writes when interestRate null and type/dueDate are 0")
    void testUpdatePawnSetting_ZeroValues() {
        PawnSetting setting = PawnSetting.builder()
                .interestRate(null)
                .interestType(0)
                .dueDate(0)
                .build();

        shopInfoService.updatePawnSetting(setting);

        verify(shopConfigService, never()).set(eq(ShopConfigKey.PAWN_INTEREST_RATE), any(java.math.BigDecimal.class));
        verify(shopConfigService, never()).set(eq(ShopConfigKey.PAWN_INTEREST_TYPE), any(Integer.class));
        verify(shopConfigService, never()).set(eq(ShopConfigKey.PAWN_DUE_DATE), any(Integer.class));
    }

    // ============= getPawnSetting Tests =============

    @Test
    @DisplayName("getPawnSetting: returns pawn setting with all fields from config")
    void testGetPawnSetting_WithInterestRate() {
        when(shopConfigService.getDecimal(ShopConfigKey.PAWN_INTEREST_RATE))
                .thenReturn(new java.math.BigDecimal("3.5"));
        when(shopConfigService.getInt(ShopConfigKey.PAWN_INTEREST_TYPE, 30)).thenReturn(30);
        when(shopConfigService.getInt(ShopConfigKey.PAWN_DUE_DATE, 30)).thenReturn(60);

        PawnSetting result = shopInfoService.getPawnSetting();

        assertThat(result.getInterestRate()).isEqualByComparingTo("3.5");
        assertThat(result.getInterestType()).isEqualTo(30);
        assertThat(result.getDueDate()).isEqualTo(60);
    }

    @Test
    @DisplayName("getPawnSetting: returns BigDecimal.ZERO for interestRate when config absent")
    void testGetPawnSetting_NullInterestRate() {
        when(shopConfigService.getDecimal(ShopConfigKey.PAWN_INTEREST_RATE)).thenReturn(null);
        when(shopConfigService.getInt(ShopConfigKey.PAWN_INTEREST_TYPE, 30)).thenReturn(30);
        when(shopConfigService.getInt(ShopConfigKey.PAWN_DUE_DATE, 30)).thenReturn(30);

        PawnSetting result = shopInfoService.getPawnSetting();

        assertThat(result.getInterestRate()).isEqualByComparingTo(java.math.BigDecimal.ZERO);
    }

    // ============= updateShopInfo: remaining config fields =============

    @Test
    @DisplayName("updateShopInfo: writes eInvoicePassword and eInvoiceKey when present")
    void testUpdateShopInfo_EInvoicePasswordAndKey() {
        ShopInfoDTO dto = ShopInfoDTO.builder()
                .eInvoicePassword("secret123")
                .eInvoiceKey("apikey456")
                .build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any())).thenReturn(testShopInfo);

        shopInfoService.updateShopInfo(dto);

        verify(shopConfigService).set(ShopConfigKey.EINVOICE_PASSWORD, "secret123");
        verify(shopConfigService).set(ShopConfigKey.EINVOICE_KEY, "apikey456");
    }

    @Test
    @DisplayName("updateShopInfo: writes invoiceSystem when present")
    void testUpdateShopInfo_InvoiceSystem() {
        ShopInfoDTO dto = ShopInfoDTO.builder().invoiceSystem("VAT").build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any())).thenReturn(testShopInfo);

        shopInfoService.updateShopInfo(dto);

        verify(shopConfigService).set(ShopConfigKey.INVOICE_SYSTEM, "VAT");
    }

    @Test
    @DisplayName("updateShopInfo: writes posMode and pawnSettings when present")
    void testUpdateShopInfo_PosAndPawnConfig() {
        ShopInfoDTO dto = ShopInfoDTO.builder()
                .posMode("TABLE")
                .pawnInterestRate(new java.math.BigDecimal("3.5"))
                .pawnInterestType(30)
                .pawnDueDate(60)
                .excludeVisibleItem(true)
                .pawnCategoryConfig("{}")
                .build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any())).thenReturn(testShopInfo);

        shopInfoService.updateShopInfo(dto);

        verify(shopConfigService).set(ShopConfigKey.POS_MODE, "TABLE");
        verify(shopConfigService).set(ShopConfigKey.PAWN_INTEREST_RATE, new java.math.BigDecimal("3.5"));
        verify(shopConfigService).set(ShopConfigKey.PAWN_INTEREST_TYPE, 30);
        verify(shopConfigService).set(ShopConfigKey.PAWN_DUE_DATE, 60);
        verify(shopConfigService).set(ShopConfigKey.PAWN_EXCLUDE_VISIBLE_ITEM, true);
        verify(shopConfigService).set(ShopConfigKey.PAWN_CATEGORY_CONFIG, "{}");
    }

    @Test
    @DisplayName("updateShopInfo: always writes cashDenominations, pawnDenominations, priceBoardCode, shopLocations")
    void testUpdateShopInfo_AlwaysWriteFields() {
        ShopInfoDTO dto = ShopInfoDTO.builder()
                .cashDenominations("500000,200000")
                .pawnDenominations("1000000")
                .priceBoardCode("BOARD_01")
                .shopLocations("[\"A\",\"B\"]")
                .build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any())).thenReturn(testShopInfo);

        shopInfoService.updateShopInfo(dto);

        verify(shopConfigService).set(ShopConfigKey.CASH_DENOMINATIONS, "500000,200000");
        verify(shopConfigService).set(ShopConfigKey.PAWN_DENOMINATIONS, "1000000");
        verify(shopConfigService).set(ShopConfigKey.PRICE_BOARD_CODE, "BOARD_01");
        verify(shopConfigService).set(ShopConfigKey.SHOP_LOCATIONS, "[\"A\",\"B\"]");
    }

    // ============= mapToPublicDTO Tests =============

    @Test
    @DisplayName("mapToPublicDTO: maps all fields from ShopInfo and configs")
    void testMapToPublicDTO_AllFields() {
        when(shopConfigService.getDouble(eq(ShopConfigKey.DEFAULT_TAX_RATE), anyDouble())).thenReturn(10.0);
        when(shopConfigService.getString(ShopConfigKey.CASH_DENOMINATIONS)).thenReturn("500000");
        when(shopConfigService.getString(eq(ShopConfigKey.POS_MODE), any())).thenReturn("STANDARD");

        PublicShopInfoDTO result = shopInfoService.mapToPublicDTO(testShopInfo);

        assertThat(result.getShopName()).isEqualTo("Test Shop");
        assertThat(result.getPhone()).isEqualTo("0123456789");
        assertThat(result.getDefaultTaxRate()).isEqualTo(10.0);
        assertThat(result.getCashDenominations()).isEqualTo("500000");
        assertThat(result.getPosMode()).isEqualTo("STANDARD");
    }

    // ============= parseTaxRateByProductType Tests =============

    @Test
    @DisplayName("parseTaxRateByProductType: returns empty map when config is null")
    void testParseTaxRateByProductType_Null_ReturnsEmpty() {
        when(shopConfigService.getString(ShopConfigKey.TAX_RATE_BY_PRODUCT_TYPE)).thenReturn(null);

        Map<String, Double> result = shopInfoService.parseTaxRateByProductType();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("parseTaxRateByProductType: returns empty map when config is blank")
    void testParseTaxRateByProductType_Blank_ReturnsEmpty() {
        when(shopConfigService.getString(ShopConfigKey.TAX_RATE_BY_PRODUCT_TYPE)).thenReturn("   ");

        Map<String, Double> result = shopInfoService.parseTaxRateByProductType();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("parseTaxRateByProductType: parses valid JSON into map")
    void testParseTaxRateByProductType_ValidJson_ReturnsParsedMap() {
        when(shopConfigService.getString(ShopConfigKey.TAX_RATE_BY_PRODUCT_TYPE))
                .thenReturn("{\"FOOD\":10.0,\"DRUG\":5.0}");

        Map<String, Double> result = shopInfoService.parseTaxRateByProductType();

        assertThat(result).containsEntry("FOOD", 10.0).containsEntry("DRUG", 5.0);
    }

    @Test
    @DisplayName("parseTaxRateByProductType: returns empty map when JSON is invalid")
    void testParseTaxRateByProductType_InvalidJson_ReturnsEmpty() {
        when(shopConfigService.getString(ShopConfigKey.TAX_RATE_BY_PRODUCT_TYPE))
                .thenReturn("{not-valid-json!!}");

        Map<String, Double> result = shopInfoService.parseTaxRateByProductType();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("updateShopInfo: serializes and writes taxRateByProductType to config")
    void testUpdateShopInfo_WithTaxRateByProductType() {
        ShopInfoDTO dto = ShopInfoDTO.builder()
                .taxRateByProductType(Map.of("FOOD", 10.0))
                .build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class))).thenReturn(testShopInfo);

        shopInfoService.updateShopInfo(dto);

        verify(shopConfigService).set(eq(ShopConfigKey.TAX_RATE_BY_PRODUCT_TYPE), anyString());
    }

    // ============= getMobileShopConfig Tests =============

    @Test
    @DisplayName("getMobileShopConfig: maps shop info fields and POS mode")
    void testGetMobileShopConfig_Success() {
        testShopInfo.setDescription("Cửa hàng vàng bạc");
        testShopInfo.setLogoUrl("https://cdn/logo.jpg");
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(testShopInfo));
        when(shopConfigService.getString(eq(ShopConfigKey.POS_MODE), eq("STANDARD"))).thenReturn("TABLE");

        MobileShopInfoDTO result = shopInfoService.getMobileShopConfig("JEWELRY");

        assertThat(result.getShopName()).isEqualTo("Test Shop");
        assertThat(result.getAddress()).isEqualTo("123 Main St");
        assertThat(result.getPhone()).isEqualTo("0123456789");
        assertThat(result.getDescription()).isEqualTo("Cửa hàng vàng bạc");
        assertThat(result.getLogoUrl()).isEqualTo("https://cdn/logo.jpg");
        assertThat(result.getShopTypeCode()).isEqualTo("JEWELRY");
        assertThat(result.getPosMode()).isEqualTo("TABLE");
    }

    @Test
    @DisplayName("getMobileShopConfig: creates shop info when none exists")
    void testGetMobileShopConfig_CreateNew() {
        ShopInfo created = new ShopInfo();
        created.setId(9L);
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.empty());
        when(shopInfoRepository.save(any(ShopInfo.class))).thenReturn(created);
        when(shopConfigService.getString(eq(ShopConfigKey.POS_MODE), eq("STANDARD"))).thenReturn("STANDARD");

        MobileShopInfoDTO result = shopInfoService.getMobileShopConfig("RETAIL");

        assertThat(result).isNotNull();
        assertThat(result.getPosMode()).isEqualTo("STANDARD");
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    // ============= updateMobileShopConfig Tests =============

    @Test
    @DisplayName("updateMobileShopConfig: updates provided fields and returns mobile config")
    void testUpdateMobileShopConfig_UpdatesFields() {
        UpdateMobileShopConfigRequest req = new UpdateMobileShopConfigRequest();
        req.setShopName("Tiệm Mới");
        req.setAddress("456 Đường Mới");
        req.setPhone("0900000000");
        req.setDescription("Mô tả mới");

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class))).thenReturn(testShopInfo);
        when(shopConfigService.getString(eq(ShopConfigKey.POS_MODE), eq("STANDARD"))).thenReturn("STANDARD");

        MobileShopInfoDTO result = shopInfoService.updateMobileShopConfig(req);

        assertThat(result).isNotNull();
        verify(shopInfoRepository).save(argThatShopName("Tiệm Mới"));
    }

    @Test
    @DisplayName("updateMobileShopConfig: skips blank shop name but keeps other fields")
    void testUpdateMobileShopConfig_BlankShopName() {
        UpdateMobileShopConfigRequest req = new UpdateMobileShopConfigRequest();
        req.setShopName("   ");
        req.setAddress("Địa chỉ");

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class))).thenReturn(testShopInfo);
        when(shopConfigService.getString(eq(ShopConfigKey.POS_MODE), eq("STANDARD"))).thenReturn("STANDARD");

        shopInfoService.updateMobileShopConfig(req);

        // shop name unchanged (blank ignored)
        verify(shopInfoRepository).save(argThatShopName("Test Shop"));
    }

    @Test
    @DisplayName("updateMobileShopConfig: all null fields leaves shop info unchanged")
    void testUpdateMobileShopConfig_AllNull() {
        UpdateMobileShopConfigRequest req = new UpdateMobileShopConfigRequest();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class))).thenReturn(testShopInfo);
        when(shopConfigService.getString(eq(ShopConfigKey.POS_MODE), eq("STANDARD"))).thenReturn("STANDARD");

        shopInfoService.updateMobileShopConfig(req);

        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    private static ShopInfo argThatShopName(String expected) {
        return org.mockito.ArgumentMatchers.argThat(s -> expected.equals(s.getShopName()));
    }

    // ============= getPosConfig Tests =============

    @Test
    @DisplayName("getPosConfig: builds DTO from config values and parses quick phrases")
    void testGetPosConfig_Success() {
        when(shopConfigService.getString(eq(ShopConfigKey.POS_MODE), eq("STANDARD"))).thenReturn("TABLE");
        when(shopConfigService.getBoolean(ShopConfigKey.AUTO_PRINT, false)).thenReturn(true);
        when(shopConfigService.getBoolean(ShopConfigKey.VAT_ENABLED, false)).thenReturn(true);
        when(shopConfigService.getString(ShopConfigKey.CASH_DENOMINATIONS)).thenReturn("500000,200000");
        when(shopConfigService.getString(ShopConfigKey.QUICK_PHRASES)).thenReturn("Cảm ơn|| Hẹn gặp lại ||");

        PosConfigDTO result = shopInfoService.getPosConfig();

        assertThat(result.getPosMode()).isEqualTo("TABLE");
        assertThat(result.getAutoPrint()).isTrue();
        assertThat(result.getVatEnabled()).isTrue();
        assertThat(result.getCashDenominations()).isEqualTo("500000,200000");
        assertThat(result.getQuickPhrases()).containsExactly("Cảm ơn", "Hẹn gặp lại");
    }

    @Test
    @DisplayName("getPosConfig: returns empty quick phrases when config blank")
    void testGetPosConfig_BlankQuickPhrases() {
        when(shopConfigService.getString(eq(ShopConfigKey.POS_MODE), eq("STANDARD"))).thenReturn("STANDARD");
        when(shopConfigService.getBoolean(ShopConfigKey.AUTO_PRINT, false)).thenReturn(false);
        when(shopConfigService.getBoolean(ShopConfigKey.VAT_ENABLED, false)).thenReturn(false);
        when(shopConfigService.getString(ShopConfigKey.CASH_DENOMINATIONS)).thenReturn(null);
        when(shopConfigService.getString(ShopConfigKey.QUICK_PHRASES)).thenReturn("   ");

        PosConfigDTO result = shopInfoService.getPosConfig();

        assertThat(result.getQuickPhrases()).isEmpty();
    }

    @Test
    @DisplayName("getPosConfig: returns empty quick phrases when config null")
    void testGetPosConfig_NullQuickPhrases() {
        when(shopConfigService.getString(eq(ShopConfigKey.POS_MODE), eq("STANDARD"))).thenReturn("STANDARD");
        when(shopConfigService.getBoolean(ShopConfigKey.AUTO_PRINT, false)).thenReturn(false);
        when(shopConfigService.getBoolean(ShopConfigKey.VAT_ENABLED, false)).thenReturn(false);
        when(shopConfigService.getString(ShopConfigKey.CASH_DENOMINATIONS)).thenReturn(null);
        when(shopConfigService.getString(ShopConfigKey.QUICK_PHRASES)).thenReturn(null);

        PosConfigDTO result = shopInfoService.getPosConfig();

        assertThat(result.getQuickPhrases()).isEmpty();
    }

    // ============= updatePosConfig Tests =============

    @Test
    @DisplayName("updatePosConfig: writes all provided fields and joins quick phrases")
    void testUpdatePosConfig_AllFields() {
        PosConfigDTO dto = PosConfigDTO.builder()
                .posMode("TABLE")
                .autoPrint(true)
                .vatEnabled(true)
                .cashDenominations("500000")
                .quickPhrases(List.of("Cảm ơn", "Hẹn gặp lại"))
                .build();

        // re-read in getPosConfig at end
        when(shopConfigService.getString(eq(ShopConfigKey.POS_MODE), eq("STANDARD"))).thenReturn("TABLE");
        when(shopConfigService.getBoolean(ShopConfigKey.AUTO_PRINT, false)).thenReturn(true);
        when(shopConfigService.getBoolean(ShopConfigKey.VAT_ENABLED, false)).thenReturn(true);
        when(shopConfigService.getString(ShopConfigKey.CASH_DENOMINATIONS)).thenReturn("500000");
        when(shopConfigService.getString(ShopConfigKey.QUICK_PHRASES)).thenReturn("Cảm ơn||Hẹn gặp lại");

        PosConfigDTO result = shopInfoService.updatePosConfig(dto);

        verify(shopConfigService).set(ShopConfigKey.POS_MODE, "TABLE");
        verify(shopConfigService).set(ShopConfigKey.AUTO_PRINT, true);
        verify(shopConfigService).set(ShopConfigKey.VAT_ENABLED, true);
        verify(shopConfigService).set(ShopConfigKey.CASH_DENOMINATIONS, "500000");
        verify(shopConfigService).set(ShopConfigKey.QUICK_PHRASES, "Cảm ơn||Hẹn gặp lại");
        assertThat(result.getQuickPhrases()).containsExactly("Cảm ơn", "Hẹn gặp lại");
    }

    @Test
    @DisplayName("updatePosConfig: skips null fields")
    void testUpdatePosConfig_NullFields() {
        PosConfigDTO dto = PosConfigDTO.builder().build();

        when(shopConfigService.getString(eq(ShopConfigKey.POS_MODE), eq("STANDARD"))).thenReturn("STANDARD");
        when(shopConfigService.getBoolean(ShopConfigKey.AUTO_PRINT, false)).thenReturn(false);
        when(shopConfigService.getBoolean(ShopConfigKey.VAT_ENABLED, false)).thenReturn(false);
        when(shopConfigService.getString(ShopConfigKey.CASH_DENOMINATIONS)).thenReturn(null);
        when(shopConfigService.getString(ShopConfigKey.QUICK_PHRASES)).thenReturn(null);

        shopInfoService.updatePosConfig(dto);

        verify(shopConfigService, never()).set(eq(ShopConfigKey.POS_MODE), anyString());
        verify(shopConfigService, never()).set(eq(ShopConfigKey.AUTO_PRINT), any(Boolean.class));
        verify(shopConfigService, never()).set(eq(ShopConfigKey.QUICK_PHRASES), anyString());
    }

    // ============= updatePawnSetting: acceptedTypes branch =============

    @Test
    @DisplayName("updatePawnSetting: writes acceptedTypes when non-null")
    void testUpdatePawnSetting_AcceptedTypes() {
        PawnSetting setting = PawnSetting.builder()
                .interestRate(null)
                .interestType(0)
                .dueDate(0)
                .acceptedTypes("GOLD,ELECTRONICS")
                .build();

        shopInfoService.updatePawnSetting(setting);

        verify(shopConfigService).set(ShopConfigKey.PAWN_ACCEPTED_TYPES, "GOLD,ELECTRONICS");
    }

    @Test
    @DisplayName("getPawnSetting: includes acceptedTypes from config")
    void testGetPawnSetting_AcceptedTypes() {
        when(shopConfigService.getDecimal(ShopConfigKey.PAWN_INTEREST_RATE)).thenReturn(java.math.BigDecimal.ZERO);
        when(shopConfigService.getInt(ShopConfigKey.PAWN_INTEREST_TYPE, 30)).thenReturn(30);
        when(shopConfigService.getInt(ShopConfigKey.PAWN_DUE_DATE, 30)).thenReturn(30);
        when(shopConfigService.getString(ShopConfigKey.PAWN_ACCEPTED_TYPES)).thenReturn("GOLD,WATCH");

        PawnSetting result = shopInfoService.getPawnSetting();

        assertThat(result.getAcceptedTypes()).isEqualTo("GOLD,WATCH");
    }

    // ============= uploadLogo Tests =============

    @Test
    @DisplayName("uploadLogo: throws on null content type")
    void testUploadLogo_NullContentType() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(file.getContentType()).thenReturn(null);
        when(messageService.getMessage("error.shop.logo.invalid.type")).thenReturn("Định dạng ảnh không hợp lệ");

        assertThatThrownBy(() -> shopInfoService.uploadLogo("JEWELRY", file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Định dạng");
    }

    @Test
    @DisplayName("uploadLogo: throws on unsupported content type")
    void testUploadLogo_UnsupportedContentType() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("application/pdf");
        when(messageService.getMessage("error.shop.logo.invalid.type")).thenReturn("Định dạng ảnh không hợp lệ");

        assertThatThrownBy(() -> shopInfoService.uploadLogo("JEWELRY", file))
                .isInstanceOf(IllegalArgumentException.class);

        verify(r2StorageService, never()).upload(anyString(), any(byte[].class), anyString());
    }

    @Test
    @DisplayName("uploadLogo: wraps IO error from image processing in RuntimeException")
    void testUploadLogo_ImageProcessingFailure() throws Exception {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getInputStream()).thenThrow(new java.io.IOException("boom"));
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.of(testShopInfo));
        lenient().when(r2StorageService.keyFromUrl(any())).thenReturn(null);
        when(messageService.getMessage("error.shop.logo.process.failed")).thenReturn("Xử lý ảnh thất bại");

        assertThatThrownBy(() -> shopInfoService.uploadLogo("JEWELRY", file))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Xử lý ảnh");
    }
}
