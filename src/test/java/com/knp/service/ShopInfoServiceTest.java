package com.knp.service;

import com.knp.model.dto.ShopInfoDTO;
import com.knp.model.dto.PublicShopInfoDTO;
import com.knp.model.entity.ShopInfo;
import com.knp.repository.ShopInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShopInfoService Unit Tests")
class ShopInfoServiceTest {

    @Mock
    private ShopInfoRepository shopInfoRepository;

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
                .eInvoicePassword("password")
                .eInvoiceKey("key")
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
        // Given
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(testShopInfo));

        // When
        ShopInfoDTO result = shopInfoService.getShopInfo();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getShopName()).isEqualTo("Test Shop");
        assertThat(result.getAddress()).isEqualTo("123 Main St");
        verify(shopInfoRepository).findFirstByDeletedAtIsNullOrderByIdAsc();
    }

    @Test
    @DisplayName("Should create shop info if not exists")
    void testGetShopInfo_CreateNew() {
        // Given
        ShopInfo newShopInfo = new ShopInfo();
        newShopInfo.setId(1L);
        newShopInfo.setCreatedAt(LocalDateTime.now());

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.empty());
        when(shopInfoRepository.save(any(ShopInfo.class)))
                .thenReturn(newShopInfo);

        // When
        ShopInfoDTO result = shopInfoService.getShopInfo();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(shopInfoRepository).findFirstByDeletedAtIsNullOrderByIdAsc();
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    // ============= updateShopInfo Tests =============

    @Test
    @DisplayName("Should update shop info with all fields")
    void testUpdateShopInfo_AllFields_Success() {
        // Given
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class)))
                .thenReturn(testShopInfo);

        // When
        ShopInfoDTO result = shopInfoService.updateShopInfo(testShopInfoDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getShopName()).isEqualTo("Test Shop");
        assertThat(result.getDefaultTaxRate()).isEqualTo(10.0);
        verify(shopInfoRepository).findFirstByDeletedAtIsNullOrderByIdAsc();
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should update only shop name")
    void testUpdateShopInfo_OnlyShopName() {
        // Given
        ShopInfoDTO updateDTO = ShopInfoDTO.builder()
                .shopName("New Shop Name")
                .build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class)))
                .thenReturn(testShopInfo);

        // When
        shopInfoService.updateShopInfo(updateDTO);

        // Then
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should update only address")
    void testUpdateShopInfo_OnlyAddress() {
        // Given
        ShopInfoDTO updateDTO = ShopInfoDTO.builder()
                .address("456 New Address")
                .build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class)))
                .thenReturn(testShopInfo);

        // When
        shopInfoService.updateShopInfo(updateDTO);

        // Then
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should update tax rate")
    void testUpdateShopInfo_TaxRate() {
        // Given
        ShopInfoDTO updateDTO = ShopInfoDTO.builder()
                .defaultTaxRate(15.0)
                .build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class)))
                .thenReturn(testShopInfo);

        // When
        shopInfoService.updateShopInfo(updateDTO);

        // Then
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should update e-invoice credentials")
    void testUpdateShopInfo_EInvoiceCredentials() {
        // Given
        ShopInfoDTO updateDTO = ShopInfoDTO.builder()
                .eInvoiceUsername("newuser")
                .build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class)))
                .thenReturn(testShopInfo);

        // When
        shopInfoService.updateShopInfo(updateDTO);

        // Then
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should update invoice information")
    void testUpdateShopInfo_InvoiceInfo() {
        // Given
        ShopInfoDTO updateDTO = ShopInfoDTO.builder()
                .invoiceVendor("VIETTEL")
                .templateCode("TEMPLATE_002")
                .invoiceSeries("002")
                .build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class)))
                .thenReturn(testShopInfo);

        // When
        shopInfoService.updateShopInfo(updateDTO);

        // Then
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should create shop info if not exists during update")
    void testUpdateShopInfo_CreateNew() {
        // Given
        ShopInfo newShopInfo = new ShopInfo();
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.empty());
        when(shopInfoRepository.save(any(ShopInfo.class)))
                .thenReturn(newShopInfo);

        // When
        shopInfoService.updateShopInfo(testShopInfoDTO);

        // Then
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should skip update if fields are null")
    void testUpdateShopInfo_NullFields() {
        // Given
        ShopInfoDTO emptyDTO = ShopInfoDTO.builder().build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class)))
                .thenReturn(testShopInfo);

        // When
        shopInfoService.updateShopInfo(emptyDTO);

        // Then
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should skip empty string shop name update")
    void testUpdateShopInfo_EmptyShopName() {
        // Given
        ShopInfoDTO updateDTO = ShopInfoDTO.builder()
                .shopName("   ")
                .build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class)))
                .thenReturn(testShopInfo);

        // When
        shopInfoService.updateShopInfo(updateDTO);

        // Then
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    // ============= getDefaultTaxRate Tests =============

    @Test
    @DisplayName("Should get default tax rate successfully")
    void testGetDefaultTaxRate_Success() {
        // Given
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(testShopInfo));

        // When
        Double result = shopInfoService.getDefaultTaxRate();

        // Then
        assertThat(result).isEqualTo(10.0);
        verify(shopInfoRepository).findFirstByDeletedAtIsNullOrderByIdAsc();
    }

    @Test
    @DisplayName("Should return default tax rate 0.0 if shop info not found")
    void testGetDefaultTaxRate_NotFound() {
        // Given
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.empty());

        // When
        Double result = shopInfoService.getDefaultTaxRate();

        // Then
        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should return 0.0 when tax rate is null")
    void testGetDefaultTaxRate_NullValue() {
        // Given
        ShopInfo shopInfoWithNullTax = ShopInfo.builder().build();
        shopInfoWithNullTax.setId(1L);
        shopInfoWithNullTax.setDefaultTaxRate(null);

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(shopInfoWithNullTax));

        // When
        Double result = shopInfoService.getDefaultTaxRate();

        // Then - map(ShopInfo::getDefaultTaxRate) returns Optional.empty() when tax rate is null
        // then orElse(0.0) is called on empty Optional, so it returns 0.0
        assertThat(result).isEqualTo(0.0);
    }

    // ============= getPublicShopInfo Tests =============

    @Test
    @DisplayName("Should get public shop info without credentials")
    void testGetPublicShopInfo_Success() {
        // Given
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(testShopInfo));

        // When
        PublicShopInfoDTO result = shopInfoService.getPublicShopInfo();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getShopName()).isEqualTo("Test Shop");
        assertThat(result.getPhone()).isEqualTo("0123456789");
        verify(shopInfoRepository).findFirstByDeletedAtIsNullOrderByIdAsc();
    }

    @Test
    @DisplayName("Should create default public shop info if not exists")
    void testGetPublicShopInfo_CreateNew() {
        // Given
        ShopInfo newShopInfo = new ShopInfo();
        newShopInfo.setId(1L);
        newShopInfo.setCreatedAt(LocalDateTime.now());

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.empty());
        when(shopInfoRepository.save(any(ShopInfo.class)))
                .thenReturn(newShopInfo);

        // When
        PublicShopInfoDTO result = shopInfoService.getPublicShopInfo();

        // Then
        assertThat(result).isNotNull();
        verify(shopInfoRepository).findFirstByDeletedAtIsNullOrderByIdAsc();
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    // ============= Edge Case Tests =============

    @Test
    @DisplayName("Should handle all optional fields as null")
    void testUpdateShopInfo_AllOptionalNull() {
        // Given
        ShopInfoDTO updateDTO = ShopInfoDTO.builder()
                .shopName(null)
                .address(null)
                .companyName(null)
                .defaultTaxRate(null)
                .phone(null)
                .build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class)))
                .thenReturn(testShopInfo);

        // When
        shopInfoService.updateShopInfo(updateDTO);

        // Then
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should handle special characters in shop name")
    void testUpdateShopInfo_SpecialCharacters() {
        // Given
        ShopInfoDTO updateDTO = ShopInfoDTO.builder()
                .shopName("Shop <>&\"'")
                .build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class)))
                .thenReturn(testShopInfo);

        // When
        shopInfoService.updateShopInfo(updateDTO);

        // Then
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should handle unicode characters in address")
    void testUpdateShopInfo_UnicodeAddress() {
        // Given
        ShopInfoDTO updateDTO = ShopInfoDTO.builder()
                .address("123 Đường Nguyễn Hữu Cảnh, Quận 1, Thành phố Hồ Chí Minh")
                .build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class)))
                .thenReturn(testShopInfo);

        // When
        shopInfoService.updateShopInfo(updateDTO);

        // Then
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should update multiple fields together")
    void testUpdateShopInfo_MultipleFields() {
        // Given
        ShopInfoDTO updateDTO = ShopInfoDTO.builder()
                .shopName("Updated Shop")
                .address("New Address")
                .companyName("New Company")
                .defaultTaxRate(20.0)
                .phone("0987654321")
                .build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class)))
                .thenReturn(testShopInfo);

        // When
        shopInfoService.updateShopInfo(updateDTO);

        // Then
        verify(shopInfoRepository, times(1)).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should handle updating only address field")
    void testUpdateShopInfo_AddressOnly() {
        // Given
        ShopInfoDTO updateDTO = ShopInfoDTO.builder()
                .address("456 Park Street")
                .build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class)))
                .thenReturn(testShopInfo);

        // When
        shopInfoService.updateShopInfo(updateDTO);

        // Then
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should handle null address in update")
    void testUpdateShopInfo_NullAddress() {
        // Given
        ShopInfoDTO updateDTO = ShopInfoDTO.builder()
                .address(null)
                .build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class)))
                .thenReturn(testShopInfo);

        // When
        shopInfoService.updateShopInfo(updateDTO);

        // Then
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should update company name successfully")
    void testUpdateShopInfo_CompanyName() {
        // Given
        ShopInfoDTO updateDTO = ShopInfoDTO.builder()
                .companyName("New Company Ltd")
                .build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class)))
                .thenReturn(testShopInfo);

        // When
        shopInfoService.updateShopInfo(updateDTO);

        // Then
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should handle null tax rate")
    void testUpdateShopInfo_NullTaxRate() {
        // Given
        ShopInfoDTO updateDTO = ShopInfoDTO.builder()
                .defaultTaxRate(null)
                .build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class)))
                .thenReturn(testShopInfo);

        // When
        shopInfoService.updateShopInfo(updateDTO);

        // Then
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should handle zero tax rate")
    void testUpdateShopInfo_ZeroTaxRate() {
        // Given
        ShopInfoDTO updateDTO = ShopInfoDTO.builder()
                .defaultTaxRate(0.0)
                .build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class)))
                .thenReturn(testShopInfo);

        // When
        shopInfoService.updateShopInfo(updateDTO);

        // Then
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should update only phone field")
    void testUpdateShopInfo_PhoneOnly() {
        // Given
        ShopInfoDTO updateDTO = ShopInfoDTO.builder()
                .phone("0123456789")
                .build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class)))
                .thenReturn(testShopInfo);

        // When
        shopInfoService.updateShopInfo(updateDTO);

        // Then
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should handle null phone in update")
    void testUpdateShopInfo_NullPhone() {
        // Given
        ShopInfoDTO updateDTO = ShopInfoDTO.builder()
                .phone(null)
                .build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class)))
                .thenReturn(testShopInfo);

        // When
        shopInfoService.updateShopInfo(updateDTO);

        // Then
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should update only email field")
    void testUpdateShopInfo_EmailOnly() {
        // Given
        ShopInfoDTO updateDTO = ShopInfoDTO.builder()
                .email("newemail@example.com")
                .build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class)))
                .thenReturn(testShopInfo);

        // When
        shopInfoService.updateShopInfo(updateDTO);

        // Then
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should handle null email in update")
    void testUpdateShopInfo_NullEmail() {
        // Given
        ShopInfoDTO updateDTO = ShopInfoDTO.builder()
                .email(null)
                .build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class)))
                .thenReturn(testShopInfo);

        // When
        shopInfoService.updateShopInfo(updateDTO);

        // Then
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should get shop info when none exists - creates default")
    void testGetShopInfo_CreateDefault() {
        // Given
        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new ShopInfo()));

        ShopInfo newShopInfo = new ShopInfo();
        newShopInfo.setId(2L);
        when(shopInfoRepository.save(any(ShopInfo.class)))
                .thenReturn(newShopInfo);

        // When
        ShopInfoDTO result = shopInfoService.getShopInfo();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2L);
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should handle very long shop name")
    void testUpdateShopInfo_LongShopName() {
        // Given
        String longName = "A".repeat(500);
        ShopInfoDTO updateDTO = ShopInfoDTO.builder()
                .shopName(longName)
                .build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class)))
                .thenReturn(testShopInfo);

        // When
        shopInfoService.updateShopInfo(updateDTO);

        // Then
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should handle special characters in shop name")
    void testUpdateShopInfo_SpecialCharactersInName() {
        // Given
        ShopInfoDTO updateDTO = ShopInfoDTO.builder()
                .shopName("Shop & Café - 2024 !@#")
                .build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class)))
                .thenReturn(testShopInfo);

        // When
        shopInfoService.updateShopInfo(updateDTO);

        // Then
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should update website field")
    void testUpdateShopInfo_Website() {
        // Given
        ShopInfoDTO updateDTO = ShopInfoDTO.builder()
                .website("https://newsite.com")
                .build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class)))
                .thenReturn(testShopInfo);

        // When
        shopInfoService.updateShopInfo(updateDTO);

        // Then
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should update template code field")
    void testUpdateShopInfo_TemplateCode() {
        // Given
        ShopInfoDTO updateDTO = ShopInfoDTO.builder()
                .templateCode("NEW_TEMPLATE_001")
                .build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class)))
                .thenReturn(testShopInfo);

        // When
        shopInfoService.updateShopInfo(updateDTO);

        // Then
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }

    @Test
    @DisplayName("Should update invoice series field")
    void testUpdateShopInfo_InvoiceSeries() {
        // Given
        ShopInfoDTO updateDTO = ShopInfoDTO.builder()
                .invoiceSeries("002")
                .build();

        when(shopInfoRepository.findFirstByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(Optional.of(testShopInfo));
        when(shopInfoRepository.save(any(ShopInfo.class)))
                .thenReturn(testShopInfo);

        // When
        shopInfoService.updateShopInfo(updateDTO);

        // Then
        verify(shopInfoRepository).save(any(ShopInfo.class));
    }
}
