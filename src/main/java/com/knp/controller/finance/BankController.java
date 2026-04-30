package com.knp.controller.finance;

import com.knp.model.dto.ApiResponse;
import com.knp.model.dto.bank.BankDTO;
import com.knp.service.finance.BankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import com.knp.annotation.RequiresFeature;

@Slf4j
@RestController
@RequestMapping("/banks")
@RequiredArgsConstructor
@RequiresFeature("SHOP_INFO")
public class BankController {

    private final BankService bankService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BankDTO>>> getAllBanks() {
        log.info("GET /api/banks - Get all banks");
        List<BankDTO> banks = bankService.getAllBanks();
        return ResponseEntity.ok(ApiResponse.success(banks, "Banks retrieved successfully"));
    }
}
