package com.knp.service;

import com.knp.model.dto.employee.CreateEmployeeRequest;
import com.knp.model.dto.employee.EmployeeDTO;
import com.knp.model.dto.employee.UpdateEmployeeRequest;
import org.springframework.data.domain.Page;

import java.util.List;

public interface EmployeeService {
    Page<EmployeeDTO> getAll(String search, int page, int size);
    List<EmployeeDTO> getAllActive();
    EmployeeDTO getById(Long id);
    EmployeeDTO getByUserId(Long userId);
    EmployeeDTO create(CreateEmployeeRequest request);
    EmployeeDTO update(Long id, UpdateEmployeeRequest request);
    void delete(Long id);
}
