package com.tappy.pos.service.finance;

import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.finance.DefaultExpenseDTO;
import com.tappy.pos.model.dto.finance.DefaultExpenseRequest;
import com.tappy.pos.model.dto.finance.ShopExpenseDTO;
import com.tappy.pos.model.dto.finance.ShopExpenseRequest;
import com.tappy.pos.model.entity.finance.DefaultExpense;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.config.AuthContext;
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.repository.finance.DefaultExpenseRepository;
import com.tappy.pos.repository.finance.ShopExpenseRepository;
import com.tappy.pos.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultExpenseServiceImpl implements DefaultExpenseService {

    private final DefaultExpenseRepository defaultExpenseRepository;
    private final ShopExpenseRepository shopExpenseRepository;
    private final ShopExpenseService shopExpenseService;
    private final TenantContext tenantContext;
    private final MessageService messageService;
    private final AuthContext authContext;
    private final ActivityLogService activityLogService;

    @Override
    @Transactional(readOnly = true)
    public List<DefaultExpenseDTO> findAll() {
        return defaultExpenseRepository.findAllActive().stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional
    public DefaultExpenseDTO create(DefaultExpenseRequest request) {
        DefaultExpense expense = DefaultExpense.builder()
                .tenantId(tenantContext.getCurrentTenantId())
                .description(request.getDescription())
                .amount(request.getAmount())
                .category(request.getCategory())
                .paymentDay(request.getPaymentDay())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .build();
        DefaultExpense saved = defaultExpenseRepository.save(expense);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.DEFAULT_EXPENSE_CREATED, "DEFAULT_EXPENSE", String.valueOf(saved.getId()),
                "Tạo chi phí định kỳ", null);
        return toDTO(saved);
    }

    @Override
    @Transactional
    public DefaultExpenseDTO update(Long id, DefaultExpenseRequest request) {
        DefaultExpense expense = findActive(id);
        expense.setDescription(request.getDescription());
        expense.setAmount(request.getAmount());
        expense.setCategory(request.getCategory());
        expense.setPaymentDay(request.getPaymentDay());
        if (request.getDisplayOrder() != null) {
            expense.setDisplayOrder(request.getDisplayOrder());
        }
        DefaultExpense saved = defaultExpenseRepository.save(expense);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.DEFAULT_EXPENSE_UPDATED, "DEFAULT_EXPENSE", String.valueOf(saved.getId()),
                "Cập nhật chi phí định kỳ", null);
        return toDTO(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        DefaultExpense expense = findActive(id);
        expense.softDelete();
        defaultExpenseRepository.save(expense);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.DEFAULT_EXPENSE_DELETED, "DEFAULT_EXPENSE", String.valueOf(id),
                "Xóa chi phí định kỳ", null);
    }

    @Override
    @Transactional
    public List<ShopExpenseDTO> cloneToMonth(String month, List<Long> ids) {
        YearMonth ym = YearMonth.parse(month);
        LocalDate firstDay = ym.atDay(1);
        LocalDate lastDay = ym.atEndOfMonth();

        List<DefaultExpense> all = defaultExpenseRepository.findAllActive();
        List<DefaultExpense> defaults = (ids != null && !ids.isEmpty())
                ? all.stream().filter(d -> ids.contains(d.getId())).toList()
                : all;
        List<ShopExpenseDTO> created = new ArrayList<>();

        for (DefaultExpense def : defaults) {
            if (shopExpenseRepository.existsByDescriptionAndDateRange(tenantContext.getCurrentTenantId(), def.getDescription(), firstDay, lastDay)) {
                log.debug("Skipping clone for '{}' — already exists in {}", def.getDescription(), month);
                continue;
            }

            int day = def.getPaymentDay() != null
                    ? Math.min(def.getPaymentDay(), lastDay.getDayOfMonth())
                    : 1;
            LocalDate expenseDate = firstDay.withDayOfMonth(day);

            ShopExpenseRequest req = new ShopExpenseRequest();
            req.setDescription(def.getDescription());
            req.setAmount(def.getAmount());
            req.setCategory(def.getCategory());
            req.setExpenseDate(expenseDate);

            try {
                created.add(shopExpenseService.create(req));
            } catch (Exception ex) {
                log.warn("Failed to clone default expense '{}': {}", def.getDescription(), ex.getMessage());
            }
        }
        return created;
    }

    private DefaultExpense findActive(Long id) {
        return defaultExpenseRepository.findById(id)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.defaultExpense.not.found", id)));
    }

    private DefaultExpenseDTO toDTO(DefaultExpense e) {
        return DefaultExpenseDTO.builder()
                .id(e.getId())
                .description(e.getDescription())
                .amount(e.getAmount())
                .category(e.getCategory())
                .categoryDisplayName(e.getCategory().getDisplayName())
                .paymentDay(e.getPaymentDay())
                .displayOrder(e.getDisplayOrder())
                .build();
    }
}
