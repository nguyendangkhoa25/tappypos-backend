package com.barbershop.service;

import com.barbershop.model.dto.*;
import com.barbershop.model.entity.Employee;
import com.barbershop.model.entity.Order;
import com.barbershop.repository.EmployeeRepository;
import com.barbershop.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final OrderRepository orderRepository;

    public EmployeeDTO createEmployee(CreateEmployeeRequest request) {
        Employee employee = Employee.builder()
                .name(request.getName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .position(request.getPosition())
                .hireDate(request.getHireDate() != null ? request.getHireDate() : LocalDate.now())
                .status(Employee.EmployeeStatus.ACTIVE)
                .totalEarned(BigDecimal.ZERO)
                .build();

        Employee saved = employeeRepository.save(employee);
        return mapToDTO(saved);
    }

    public Page<EmployeeDTO> getAllEmployees(Pageable pageable) {
        Page<Employee> employees = employeeRepository.findAllActive(pageable);
        return employees.map(this::mapToDTO);
    }

    public EmployeeDTO getEmployeeById(Long id) {
        Employee employee = employeeRepository.findByIdActive(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + id));
        return mapToDTO(employee);
    }

    public EmployeeDTO updateEmployee(Long id, UpdateEmployeeRequest request) {
        Employee employee = employeeRepository.findByIdActive(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + id));

        if (request.getName() != null) {
            employee.setName(request.getName());
        }
        if (request.getEmail() != null) {
            employee.setEmail(request.getEmail());
        }
        if (request.getPosition() != null) {
            employee.setPosition(request.getPosition());
        }
        if (request.getStatus() != null) {
            employee.setStatus(Employee.EmployeeStatus.valueOf(request.getStatus()));
        }

        Employee updated = employeeRepository.save(employee);
        return mapToDTO(updated);
    }

    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findByIdActive(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + id));
        employee.softDelete();
        employeeRepository.save(employee);
    }

    public EmployeeEarningsDTO getEmployeeEarnings(Long employeeId) {
        Employee employee = employeeRepository.findByIdActive(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));

        List<Order> completedOrders = orderRepository.findCompletedOrdersByEmployee(employeeId);
        BigDecimal totalEarned = completedOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return EmployeeEarningsDTO.builder()
                .employeeId(employeeId)
                .employeeName(employee.getName())
                .totalEarned(totalEarned)
                .completedOrderCount(completedOrders.size())
                .build();
    }

    public Page<EmployeeDTO> searchEmployees(String keyword, Pageable pageable) {
        Page<Employee> employees = employeeRepository.searchByKeyword(keyword, pageable);
        return employees.map(this::mapToDTO);
    }

    private EmployeeDTO mapToDTO(Employee employee) {
        return EmployeeDTO.builder()
                .id(employee.getId())
                .name(employee.getName())
                .phone(employee.getPhone())
                .email(employee.getEmail())
                .position(employee.getPosition())
                .hireDate(employee.getHireDate())
                .status(employee.getStatus().name())
                .totalEarned(employee.getTotalEarned())
                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt())
                .build();
    }
}

