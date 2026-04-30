package com.knp.service.employee;

import com.knp.exception.BadRequestException;
import com.knp.service.MessageService;
import com.knp.exception.ResourceNotFoundException;
import com.knp.model.dto.employee.CreateEmployeeRequest;
import com.knp.model.dto.employee.EmployeeDTO;
import com.knp.model.dto.employee.UpdateEmployeeRequest;
import com.knp.model.entity.employee.Employee;
import com.knp.model.entity.auth.User;
import com.knp.model.enums.EmployeePosition;
import com.knp.repository.employee.EmployeeRepository;
import com.knp.repository.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

        EmployeePosition position = parsePosition(request.getPosition());

        if (request.getUserId() != null) {
            if (employeeRepository.existsByUserId(request.getUserId())) {
                throw new BadRequestException(messageService.getMessage("error.employee.user.already.linked", request.getUserId()));
            }
            userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            messageService.getMessage("error.user.not.found", request.getUserId())));
        }

        Employee employee = Employee.builder()
                .fullName(request.getFullName())
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
                .build();

        Employee saved = employeeRepository.save(employee);
        log.info("Employee created with id: {}", saved.getId());
        return toDTO(saved);
    }

    @Override
    public EmployeeDTO update(Long id, UpdateEmployeeRequest request) {
        log.info("Updating employee id: {}", id);
        Employee employee = findById(id);

        if (request.getFullName() != null) employee.setFullName(request.getFullName());
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

        return toDTO(employeeRepository.save(employee));
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
                .build();
    }
}
