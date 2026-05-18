package com.tappy.pos.controller.employee;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.employee.CreateAdvanceRequest;
import com.tappy.pos.model.dto.employee.SalaryAdvanceDTO;
import com.tappy.pos.service.employee.SalaryAdvanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/salary/advances")
@RequiredArgsConstructor
@RequiresFeature("SALARY")
public class SalaryAdvanceController {

    private final SalaryAdvanceService advanceService;

    @PostMapping
    public ResponseEntity<SalaryAdvanceDTO> create(@RequestBody CreateAdvanceRequest request) {
        log.info("Endpoint: POST /salary/advances employee={} amount={}", request.getEmployeeId(), request.getAmount());
        return ResponseEntity.ok(advanceService.createAdvance(request));
    }

    @GetMapping
    public ResponseEntity<Page<SalaryAdvanceDTO>> list(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Endpoint: GET /salary/advances employeeId={}", employeeId);
        return ResponseEntity.ok(advanceService.getAdvances(employeeId, page, size));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("Endpoint: DELETE /salary/advances/{}", id);
        advanceService.deleteAdvance(id);
        return ResponseEntity.noContent().build();
    }
}
