package com.tappy.pos.controller.finance;

import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.bank.BankDTO;
import com.tappy.pos.service.finance.BankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import com.tappy.pos.annotation.RequiresFeature;

@Slf4j
@RestController
@RequestMapping("/banks")
@RequiredArgsConstructor
@RequiresFeature("BANK_ACCOUNT")
public class BankController {

    private final BankService bankService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BankDTO>>> getAllBanks() {
        log.info("GET /api/banks - Get all banks");
        List<BankDTO> banks = bankService.getAllBanks();
        return ResponseEntity.ok(ApiResponse.success(banks, "Banks retrieved successfully"));
    }
}
