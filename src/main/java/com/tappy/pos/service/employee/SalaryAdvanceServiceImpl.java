package com.tappy.pos.service.employee;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.employee.CreateAdvanceRequest;
import com.tappy.pos.model.dto.employee.SalaryAdvanceDTO;
import com.tappy.pos.model.entity.employee.Employee;
import com.tappy.pos.model.entity.employee.SalaryAdvance;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.employee.EmployeeRepository;
import com.tappy.pos.repository.employee.SalaryAdvanceRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SalaryAdvanceServiceImpl implements SalaryAdvanceService {

    private final SalaryAdvanceRepository advanceRepository;
    private final EmployeeRepository      employeeRepository;
    private final MessageService          messageService;
    private final TenantContext           tenantContext;
    private final ActivityLogService      activityLogService;

    @Override
    public SalaryAdvanceDTO createAdvance(CreateAdvanceRequest req) {
        if (req.getAmount() == null || req.getAmount().signum() <= 0)
            throw new BadRequestException(messageService.getMessage("error.advance.amount.invalid"));
        if (req.getAdvanceDate() == null)
            throw new BadRequestException(messageService.getMessage("error.advance.date.required"));

        Employee emp = employeeRepository.findById(req.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("employee.not.found")));

        String actor = SecurityContextHolder.getContext().getAuthentication().getName();

        SalaryAdvance advance = SalaryAdvance.builder()
                .tenantId(tenantContext.getCurrentTenantId())
                .employeeId(emp.getId())
                .employeeName(emp.getFullName())
                .amount(req.getAmount())
                .advanceDate(req.getAdvanceDate() != null ? req.getAdvanceDate() : LocalDate.now())
                .note(req.getNote())
                .deducted(false)
                .createdBy(actor)
                .build();

        SalaryAdvance saved = advanceRepository.save(advance);
        activityLogService.logAsync(
                tenantContext.getCurrentTenantId(), actor, null,
                ActivityAction.SALARY_ADVANCE_CREATED, "SALARY_ADVANCE",
                saved.getId().toString(),
                "Ứng lương " + emp.getFullName() + " - " + req.getAmount().toPlainString() + "₫", null);

        return mapToDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SalaryAdvanceDTO> getAdvances(Long employeeId, int page, int size) {
        return advanceRepository.findFiltered(employeeId, PageRequest.of(page, size))
                .map(this::mapToDTO);
    }

    @Override
    public void deleteAdvance(Long id) {
        SalaryAdvance advance = advanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.advance.not.found", id)));
        if (advance.isDeducted())
            throw new BadRequestException(messageService.getMessage("error.advance.already.deducted"));

        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        activityLogService.logAsync(
                tenantContext.getCurrentTenantId(), actor, null,
                ActivityAction.SALARY_ADVANCE_DELETED, "SALARY_ADVANCE",
                advance.getId().toString(),
                "Xóa ứng lương " + advance.getEmployeeName(), null);

        advanceRepository.delete(advance);
    }

    private SalaryAdvanceDTO mapToDTO(SalaryAdvance a) {
        return SalaryAdvanceDTO.builder()
                .id(a.getId())
                .employeeId(a.getEmployeeId())
                .employeeName(a.getEmployeeName())
                .amount(a.getAmount())
                .advanceDate(a.getAdvanceDate())
                .note(a.getNote())
                .salaryId(a.getSalaryId())
                .deducted(a.isDeducted())
                .createdBy(a.getCreatedBy())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
