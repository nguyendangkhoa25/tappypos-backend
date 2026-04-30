package com.knp.controller.finance;

import com.knp.model.dto.ApiResponse;
import com.knp.model.dto.bank.BankAccountDTO;
import com.knp.model.dto.bank.SaveBankAccountRequest;
import com.knp.service.finance.BankAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.knp.annotation.RequiresFeature;

@RestController
@RequestMapping("/bank-accounts")
@RequiredArgsConstructor
@RequiresFeature("BANK_ACCOUNT")
public class BankAccountController {

    private final BankAccountService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BankAccountDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(service.getAll()));
    }

    @GetMapping("/default")
    public ResponseEntity<ApiResponse<BankAccountDTO>> getDefault() {
        return ResponseEntity.ok(ApiResponse.success(service.getDefault()));
    }

    @GetMapping("/pos-default")
    @RequiresFeature("POS")
    public ResponseEntity<ApiResponse<BankAccountDTO>> getPosDefault() {
        return ResponseEntity.ok(ApiResponse.success(service.getDefault()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BankAccountDTO>> create(@RequestBody @Valid SaveBankAccountRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.create(req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BankAccountDTO>> update(
            @PathVariable Long id, @RequestBody @Valid SaveBankAccountRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.update(id, req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<ApiResponse<BankAccountDTO>> setDefault(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.setDefault(id)));
    }
}
