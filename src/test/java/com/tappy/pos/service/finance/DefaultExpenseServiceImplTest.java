package com.tappy.pos.service.finance;

import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.finance.DefaultExpenseDTO;
import com.tappy.pos.model.dto.finance.DefaultExpenseRequest;
import com.tappy.pos.model.dto.finance.ShopExpenseDTO;
import com.tappy.pos.model.entity.finance.DefaultExpense;
import com.tappy.pos.model.enums.ExpenseCategory;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.finance.DefaultExpenseRepository;
import com.tappy.pos.repository.finance.ShopExpenseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultExpenseServiceImpl Unit Tests")
class DefaultExpenseServiceImplTest {

    @Mock private DefaultExpenseRepository defaultExpenseRepository;
    @Mock private ShopExpenseRepository shopExpenseRepository;
    @Mock private ShopExpenseService shopExpenseService;
    @Mock private TenantContext tenantContext;
    @Mock private com.tappy.pos.service.MessageService messageService;

    @InjectMocks
    private DefaultExpenseServiceImpl service;

    private DefaultExpense expense(Long id, String desc, Integer paymentDay) {
        DefaultExpense e = DefaultExpense.builder()
                .tenantId("shop-1")
                .description(desc)
                .amount(new BigDecimal("500000"))
                .category(ExpenseCategory.ELECTRICITY)
                .paymentDay(paymentDay)
                .displayOrder(1)
                .build();
        e.setId(id);
        return e;
    }

    @Test
    @DisplayName("findAll: maps active default expenses")
    void findAll() {
        when(defaultExpenseRepository.findAllActive()).thenReturn(List.of(expense(1L, "Tiền điện", 5)));

        List<DefaultExpenseDTO> result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategoryDisplayName()).isEqualTo("Tiền điện");
    }

    @Test
    @DisplayName("create: builds and persists a default expense")
    void create() {
        when(tenantContext.getCurrentTenantId()).thenReturn("shop-1");
        when(defaultExpenseRepository.save(any(DefaultExpense.class))).thenAnswer(inv -> {
            DefaultExpense e = inv.getArgument(0);
            e.setId(7L);
            return e;
        });
        DefaultExpenseRequest req = new DefaultExpenseRequest();
        req.setDescription("Tiền nước");
        req.setAmount(new BigDecimal("200000"));
        req.setCategory(ExpenseCategory.WATER);
        req.setPaymentDay(10);

        DefaultExpenseDTO dto = service.create(req);

        assertThat(dto.getId()).isEqualTo(7L);
        assertThat(dto.getDescription()).isEqualTo("Tiền nước");
    }

    @Test
    @DisplayName("update: overwrites fields")
    void update() {
        DefaultExpense existing = expense(1L, "Old", 5);
        when(defaultExpenseRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(defaultExpenseRepository.save(any(DefaultExpense.class))).thenAnswer(inv -> inv.getArgument(0));
        DefaultExpenseRequest req = new DefaultExpenseRequest();
        req.setDescription("Tiền thuê");
        req.setAmount(new BigDecimal("9000000"));
        req.setCategory(ExpenseCategory.RENT);
        req.setPaymentDay(1);

        DefaultExpenseDTO dto = service.update(1L, req);

        assertThat(dto.getDescription()).isEqualTo("Tiền thuê");
        assertThat(dto.getCategory()).isEqualTo(ExpenseCategory.RENT);
    }

    @Test
    @DisplayName("update: not found → ResourceNotFoundException")
    void update_notFound() {
        when(defaultExpenseRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(99L, new DefaultExpenseRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("delete: soft-deletes the expense")
    void delete() {
        DefaultExpense e = expense(1L, "X", 5);
        when(defaultExpenseRepository.findById(1L)).thenReturn(Optional.of(e));

        service.delete(1L);

        verify(defaultExpenseRepository).save(e);
    }

    @Test
    @DisplayName("cloneToMonth: creates shop expenses for selected defaults not already present")
    void cloneToMonth_createsMissing() {
        when(tenantContext.getCurrentTenantId()).thenReturn("shop-1");
        when(defaultExpenseRepository.findAllActive()).thenReturn(List.of(expense(1L, "Tiền điện", 5)));
        when(shopExpenseRepository.existsByDescriptionAndDateRange(eq("shop-1"), eq("Tiền điện"), any(), any()))
                .thenReturn(false);
        when(shopExpenseService.create(any())).thenReturn(mock(ShopExpenseDTO.class));

        List<ShopExpenseDTO> created = service.cloneToMonth("2026-06", List.of(1L));

        assertThat(created).hasSize(1);
        verify(shopExpenseService).create(any());
    }

    @Test
    @DisplayName("cloneToMonth: skips defaults already present in the month")
    void cloneToMonth_skipsExisting() {
        when(tenantContext.getCurrentTenantId()).thenReturn("shop-1");
        when(defaultExpenseRepository.findAllActive()).thenReturn(List.of(expense(1L, "Tiền điện", 5)));
        when(shopExpenseRepository.existsByDescriptionAndDateRange(eq("shop-1"), eq("Tiền điện"), any(), any()))
                .thenReturn(true);

        List<ShopExpenseDTO> created = service.cloneToMonth("2026-06", null);

        assertThat(created).isEmpty();
        verify(shopExpenseService, never()).create(any());
    }
}
