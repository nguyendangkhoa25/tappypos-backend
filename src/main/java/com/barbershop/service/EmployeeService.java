package com.barbershop.service;

import com.barbershop.model.dto.employee.CreateEmployeeRequest;
import com.barbershop.model.dto.employee.EmployeeDTO;
import com.barbershop.model.dto.employee.EmployeeEarningsDTO;
import com.barbershop.model.dto.employee.UpdateEmployeeRequest;
import com.barbershop.model.entity.Employee;
import com.barbershop.model.entity.Order;
import com.barbershop.repository.EmployeeRepository;
import com.barbershop.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final OrderRepository orderRepository;

    public EmployeeDTO createEmployee(CreateEmployeeRequest request) {
        log.info("Request: Create new employee - name: {}, phone: {}, email: {}, position: {}",
                request.getName(), request.getPhone(), request.getEmail(), request.getPosition());

        Employee employee = Employee.builder()
                .name(request.getName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .position(request.getPosition())
                .hireDate(request.getHireDate() != null ? request.getHireDate() : LocalDate.now())
                .status(Employee.EmployeeStatus.ACTIVE)
                .description(request.getDescription())
                .baseSalary(request.getBaseSalary() != null ? request.getBaseSalary() : BigDecimal.ZERO)
                .totalEarned(BigDecimal.ZERO)
                .build();

        Employee saved = employeeRepository.save(employee);
        log.info("Employee created successfully - id: {}, name: {}", saved.getId(), saved.getName());
        return mapToDTO(saved);
    }

    public Page<EmployeeDTO> getAllEmployees(String searchTerm, String status, String sortBy, String sortDirection, Pageable pageable) {
        log.info("Request: Get all employees - search: {}, status: {}, sortBy: {}, sortDirection: {}, page: {}, size: {}",
                searchTerm, status, sortBy, sortDirection, pageable.getPageNumber(), pageable.getPageSize());

        // Create Sort object with custom sorting
        Sort.Direction direction = Sort.Direction.fromString(
                sortDirection != null && sortDirection.equalsIgnoreCase("ASC") ? "ASC" : "DESC");
        Sort sort = Sort.by(direction, sortBy != null && !sortBy.trim().isEmpty() ? sortBy : "id");

        // Create new Pageable with custom sort
        Pageable pageableWithSort = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort);

        // Convert status string to EmployeeStatus enum (null if not provided)
        Employee.EmployeeStatus statusFilter = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                statusFilter = Employee.EmployeeStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid employee status provided: {}", status);
            }
        }

        Page<Employee> employees;

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            log.debug("Searching employees by keyword: {}", searchTerm);
            // Search by text - will return all matching employees
            employees = employeeRepository.searchByKeyword(searchTerm, pageableWithSort);
            // Note: If status filter is also needed, client should be notified that search takes precedence
        } else if (statusFilter != null) {
            log.debug("Filtering employees by status: {}", statusFilter);
            // Filter by status only
            employees = employeeRepository.findByStatus(statusFilter, pageableWithSort);
        } else {
            log.debug("Retrieving all active employees");
            // Get all active employees with pagination
            employees = employeeRepository.findAllActive(pageableWithSort);
        }

        log.info("Retrieved {} employees from page {}", employees.getContent().size(), pageable.getPageNumber());
        return employees.map(this::mapToDTO);
    }

    public EmployeeDTO getEmployeeById(Long id) {
        log.info("Request: Get employee - id: {}", id);
        Employee employee = employeeRepository.findByIdActive(id)
                .orElseThrow(() -> {
                    log.error("Employee not found - id: {}", id);
                    return new RuntimeException("Employee not found with id: " + id);
                });
        log.info("Retrieved employee - id: {}, name: {}", employee.getId(), employee.getName());
        return mapToDTO(employee);
    }

    public EmployeeDTO updateEmployee(Long id, UpdateEmployeeRequest request) {
        log.info("Request: Update employee - id: {}", id);
        Employee employee = employeeRepository.findByIdActive(id)
                .orElseThrow(() -> {
                    log.error("Employee not found for update - id: {}", id);
                    return new RuntimeException("Employee not found with id: " + id);
                });

        if (request.getName() != null) {
            log.debug("Updating name - id: {}, old: {}, new: {}", id, employee.getName(), request.getName());
            employee.setName(request.getName());
        }
        if (request.getEmail() != null) {
            log.debug("Updating email - id: {}, old: {}, new: {}", id, employee.getEmail(), request.getEmail());
            employee.setEmail(request.getEmail());
        }
        if (request.getPosition() != null) {
            log.debug("Updating position - id: {}, old: {}, new: {}", id, employee.getPosition(), request.getPosition());
            employee.setPosition(request.getPosition());
        }
        if (request.getStatus() != null) {
            log.debug("Updating status - id: {}, old: {}, new: {}", id, employee.getStatus(), request.getStatus());
            employee.setStatus(Employee.EmployeeStatus.valueOf(request.getStatus()));
        }
        if (request.getDescription() != null) {
            log.debug("Updating description - id: {}", id);
            employee.setDescription(request.getDescription());
        }
        if (request.getBaseSalary() != null) {
            log.debug("Updating base salary - id: {}, old: {}, new: {}", id, employee.getBaseSalary(), request.getBaseSalary());
            employee.setBaseSalary(request.getBaseSalary());
        }

        Employee updated = employeeRepository.save(employee);
        log.info("Employee updated successfully - id: {}, name: {}", updated.getId(), updated.getName());
        return mapToDTO(updated);
    }

    public void deleteEmployee(Long id) {
        log.info("Request: Delete employee - id: {}", id);
        Employee employee = employeeRepository.findByIdActive(id)
                .orElseThrow(() -> {
                    log.error("Employee not found for deletion - id: {}", id);
                    return new RuntimeException("Employee not found with id: " + id);
                });
        employee.softDelete();
        employeeRepository.save(employee);
        log.info("Employee deleted successfully (soft delete) - id: {}, name: {}", employee.getId(), employee.getName());
    }

    public EmployeeEarningsDTO getEmployeeEarnings(Long employeeId) {
        log.info("Request: Get employee earnings - id: {}", employeeId);
        Employee employee = employeeRepository.findByIdActive(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found for earnings calculation - id: {}", employeeId);
                    return new RuntimeException("Employee not found with id: " + employeeId);
                });

        List<Order> completedOrders = orderRepository.findCompletedOrdersByEmployee(employeeId);
        log.debug("Found {} completed orders for employee - id: {}", completedOrders.size(), employeeId);

        BigDecimal totalEarned = completedOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Employee earnings calculated - id: {}, totalEarned: {}, completedOrderCount: {}",
                employeeId, totalEarned, completedOrders.size());

        return EmployeeEarningsDTO.builder()
                .employeeId(employeeId)
                .employeeName(employee.getName())
                .totalEarned(totalEarned)
                .completedOrderCount(completedOrders.size())
                .build();
    }

    private EmployeeDTO mapToDTO(Employee employee) {
        log.debug("Converting Employee to DTO - id: {}, name: {}", employee.getId(), employee.getName());
        return EmployeeDTO.builder()
                .id(employee.getId())
                .name(employee.getName())
                .phone(employee.getPhone())
                .email(employee.getEmail())
                .position(employee.getPosition())
                .hireDate(employee.getHireDate())
                .status(employee.getStatus().name())
                .description(employee.getDescription())
                .baseSalary(employee.getBaseSalary())
                .totalEarned(employee.getTotalEarned())
                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt())
                .build();
    }
}

