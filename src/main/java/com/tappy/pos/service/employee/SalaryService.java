package com.tappy.pos.service.employee;

import com.tappy.pos.model.dto.employee.ApproveSalaryRequest;
import com.tappy.pos.model.dto.employee.GenerateSalaryRequest;
import com.tappy.pos.model.dto.employee.PaySalaryRequest;
import com.tappy.pos.model.dto.employee.SalaryAdjustmentDTO;
import com.tappy.pos.model.dto.employee.SalaryAdjustmentRequest;
import com.tappy.pos.model.dto.employee.SalaryDTO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface SalaryService {
    List<SalaryDTO> generatePayroll(GenerateSalaryRequest request);
    Page<SalaryDTO> getSalaries(String status, Integer year, Integer month, int page, int size);
    SalaryDTO getSalaryDetail(Long id);
    SalaryDTO approve(Long id, ApproveSalaryRequest request);
    SalaryDTO markPaid(Long id, PaySalaryRequest request);
    void delete(Long id);
    SalaryAdjustmentDTO addAdjustment(Long salaryId, SalaryAdjustmentRequest req);
    void removeAdjustment(Long salaryId, Long adjId);
}
