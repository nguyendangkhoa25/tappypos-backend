package com.knp.service;

import com.knp.model.dto.bank.BankDTO;
import com.knp.model.entity.Bank;
import com.knp.multitenant.TenantContext;
import com.knp.repository.BankRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BankServiceImpl Unit Tests")
class BankServiceImplTest {

    @Mock
    private BankRepository bankRepository;

    @Mock
    private TenantContext tenantContext;

    @InjectMocks
    private BankServiceImpl bankService;

    private Bank bankVcb;
    private Bank bankAcb;

    @BeforeEach
    void setUp() {
        bankVcb = Bank.builder()
                .code("VCB")
                .name("Ngân hàng TMCP Ngoại Thương Việt Nam")
                .shortName("Vietcombank")
                .sortOrder(1)
                .isActive(true)
                .build();
        bankVcb.setId(1L);

        bankAcb = Bank.builder()
                .code("ACB")
                .name("Ngân hàng TMCP Á Châu")
                .shortName("ACB")
                .sortOrder(2)
                .isActive(true)
                .build();
        bankAcb.setId(2L);
    }

    @Test
    @DisplayName("Should return all active banks ordered by sort order")
    void testGetAllBanks_ReturnsSortedList() {
        when(bankRepository.findAllActiveOrderBySortOrder()).thenReturn(List.of(bankVcb, bankAcb));

        List<BankDTO> result = bankService.getAllBanks();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCode()).isEqualTo("VCB");
        assertThat(result.get(1).getCode()).isEqualTo("ACB");
        verify(bankRepository).findAllActiveOrderBySortOrder();
    }

    @Test
    @DisplayName("Should map all fields from entity to DTO")
    void testGetAllBanks_MapsFieldsCorrectly() {
        when(bankRepository.findAllActiveOrderBySortOrder()).thenReturn(List.of(bankVcb));

        List<BankDTO> result = bankService.getAllBanks();

        BankDTO dto = result.get(0);
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getCode()).isEqualTo("VCB");
        assertThat(dto.getName()).isEqualTo("Ngân hàng TMCP Ngoại Thương Việt Nam");
        assertThat(dto.getShortName()).isEqualTo("Vietcombank");
        assertThat(dto.getSortOrder()).isEqualTo(1);
        assertThat(dto.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Should return empty list when no active banks")
    void testGetAllBanks_EmptyList() {
        when(bankRepository.findAllActiveOrderBySortOrder()).thenReturn(Collections.emptyList());

        List<BankDTO> result = bankService.getAllBanks();

        assertThat(result).isEmpty();
        verify(bankRepository).findAllActiveOrderBySortOrder();
    }

    @Test
    @DisplayName("Should handle bank with null shortName")
    void testGetAllBanks_NullShortName() {
        Bank bankNoShortName = Bank.builder()
                .code("TEST")
                .name("Test Bank")
                .shortName(null)
                .sortOrder(10)
                .isActive(true)
                .build();
        bankNoShortName.setId(3L);
        when(bankRepository.findAllActiveOrderBySortOrder()).thenReturn(List.of(bankNoShortName));

        List<BankDTO> result = bankService.getAllBanks();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getShortName()).isNull();
    }
}
