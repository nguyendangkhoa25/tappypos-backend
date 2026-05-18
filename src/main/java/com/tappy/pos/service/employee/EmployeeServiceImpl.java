package com.tappy.pos.service.employee;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.employee.CreateEmployeeRequest;
import com.tappy.pos.model.dto.employee.EmployeeDTO;
import com.tappy.pos.model.dto.employee.UpdateEmployeeRequest;
import com.tappy.pos.model.entity.employee.Employee;
import com.tappy.pos.model.entity.auth.User;
import com.tappy.pos.model.enums.EmployeePosition;
import com.tappy.pos.repository.employee.EmployeeRepository;
import com.tappy.pos.repository.auth.UserRepository;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.model.enums.ActivityAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final MessageService messageService;
    private final TenantContext tenantContext;
    private final ActivityLogService activityLogService;

    @Override
    public Page<EmployeeDTO> getAll(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return employeeRepository.findAllWithSearch(search, pageable).map(this::toDTO);
    }

    @Override
    public List<EmployeeDTO> getAllActive() {
        return employeeRepository.findAllActive().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public EmployeeDTO getById(Long id) {
        return toDTO(findById(id));
    }

    @Override
    public EmployeeDTO getByUserId(Long userId) {
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.employee.not.found", userId)));
        return toDTO(employee);
    }

    @Override
    public EmployeeDTO create(CreateEmployeeRequest request) {
        log.info("Creating employee: {}", request.getFullName());

        EmployeePosition position = request.getPosition() != null ? parsePosition(request.getPosition()) : null;

        if (request.getUserId() != null) {
            if (employeeRepository.existsByUserId(request.getUserId())) {
                throw new BadRequestException(messageService.getMessage("error.employee.user.already.linked", request.getUserId()));
            }
            userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            messageService.getMessage("error.user.not.found", request.getUserId())));
        }

        Employee employee = Employee.builder()
                .tenantId(tenantContext.getCurrentTenantId())
                .fullName(request.getFullName())
                .nickName(request.getNickName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .position(position)
                .department(request.getDepartment())
                .hireDate(request.getHireDate())
                .active(request.getActive() != null ? request.getActive() : true)
                .baseWage(request.getBaseWage())
                .commissionRate(request.getCommissionRate())
                .notes(request.getNotes())
                .avatar(request.getAvatar())
                .userId(request.getUserId())
                .idCardNumber(request.getIdCardNumber())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .permanentAddress(request.getPermanentAddress())
                .idCardIssuedDate(request.getIdCardIssuedDate())
                .idCardIssuedPlace(request.getIdCardIssuedPlace())
                .idCardFrontImage(request.getIdCardFrontImage())
                .idCardBackImage(request.getIdCardBackImage())
                .build();

        Employee saved = employeeRepository.save(employee);
        log.info("Employee created with id: {}", saved.getId());

        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), actor, null,
                ActivityAction.EMPLOYEE_CREATED, "EMPLOYEE", String.valueOf(saved.getId()),
                "Thêm nhân viên: " + saved.getFullName(), null);

        return toDTO(saved);
    }

    @Override
    public EmployeeDTO update(Long id, UpdateEmployeeRequest request) {
        log.info("Updating employee id: {}", id);
        Employee employee = findById(id);

        if (request.getFullName() != null) employee.setFullName(request.getFullName());
        if (request.getNickName() != null) employee.setNickName(request.getNickName());
        if (request.getPhone() != null) employee.setPhone(request.getPhone());
        if (request.getEmail() != null) employee.setEmail(request.getEmail());
        if (request.getPosition() != null) employee.setPosition(parsePosition(request.getPosition()));
        if (request.getDepartment() != null) employee.setDepartment(request.getDepartment());
        if (request.getHireDate() != null) employee.setHireDate(request.getHireDate());
        if (request.getActive() != null) employee.setActive(request.getActive());
        if (request.getBaseWage() != null) employee.setBaseWage(request.getBaseWage());
        if (request.getCommissionRate() != null) employee.setCommissionRate(request.getCommissionRate());
        if (request.getNotes() != null) employee.setNotes(request.getNotes());
        if (request.getAvatar() != null) employee.setAvatar(request.getAvatar());
        if (request.getIdCardNumber() != null) employee.setIdCardNumber(request.getIdCardNumber());
        if (request.getDateOfBirth() != null) employee.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null) employee.setGender(request.getGender());
        if (request.getPermanentAddress() != null) employee.setPermanentAddress(request.getPermanentAddress());
        if (request.getIdCardIssuedDate() != null) employee.setIdCardIssuedDate(request.getIdCardIssuedDate());
        if (request.getIdCardIssuedPlace() != null) employee.setIdCardIssuedPlace(request.getIdCardIssuedPlace());
        if (request.getIdCardFrontImage() != null) employee.setIdCardFrontImage(request.getIdCardFrontImage());
        if (request.getIdCardBackImage() != null) employee.setIdCardBackImage(request.getIdCardBackImage());

        if (request.getUserId() != null) {
            Long newUserId = request.getUserId();
            if (!newUserId.equals(employee.getUserId())) {
                if (employeeRepository.existsByUserId(newUserId)) {
                    throw new BadRequestException(messageService.getMessage("error.employee.user.already.linked", newUserId));
                }
                userRepository.findById(newUserId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                messageService.getMessage("error.user.not.found", newUserId)));
                employee.setUserId(newUserId);
            }
        }

        Employee updated = employeeRepository.save(employee);

        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), actor, null,
                ActivityAction.EMPLOYEE_UPDATED, "EMPLOYEE", String.valueOf(updated.getId()),
                "Cập nhật nhân viên: " + updated.getFullName(), null);

        return toDTO(updated);
    }

    @Override
    public void delete(Long id) {
        Employee employee = findById(id);
        employee.softDelete();
        employeeRepository.save(employee);
        log.info("Employee soft-deleted id: {}", id);
    }

    private Employee findById(Long id) {
        return employeeRepository.findById(id)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.employee.not.found", id)));
    }

    private EmployeePosition parsePosition(String position) {
        try {
            return EmployeePosition.valueOf(position.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(messageService.getMessage("error.employee.position.invalid", position));
        }
    }

    public EmployeeDTO toDTO(Employee e) {
        String username = null;
        if (e.getUserId() != null) {
            try {
                username = userRepository.findById(e.getUserId())
                        .map(User::getUsername)
                        .orElse(null);
            } catch (Exception ignored) { /* user lookup is best-effort */ }
        }
        return EmployeeDTO.builder()
                .id(e.getId())
                .fullName(e.getFullName())
                .nickName(e.getNickName())
                .phone(e.getPhone())
                .email(e.getEmail())
                .position(e.getPosition() != null ? e.getPosition().name() : null)
                .department(e.getDepartment())
                .hireDate(e.getHireDate())
                .active(e.getActive())
                .baseWage(e.getBaseWage())
                .commissionRate(e.getCommissionRate())
                .notes(e.getNotes())
                .avatar(e.getAvatar())
                .userId(e.getUserId())
                .username(username)
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .idCardNumber(e.getIdCardNumber())
                .dateOfBirth(e.getDateOfBirth())
                .gender(e.getGender())
                .permanentAddress(e.getPermanentAddress())
                .idCardIssuedDate(e.getIdCardIssuedDate())
                .idCardIssuedPlace(e.getIdCardIssuedPlace())
                .idCardFrontImage(e.getIdCardFrontImage())
                .idCardBackImage(e.getIdCardBackImage())
                .build();
    }
}
