package com.tappy.pos.service.finance;

import com.tappy.pos.model.dto.bank.BankAccountDTO;
import com.tappy.pos.model.dto.bank.SaveBankAccountRequest;
import com.tappy.pos.model.entity.finance.BankAccount;
import com.tappy.pos.repository.finance.BankAccountRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.config.AuthContext;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.service.audit.ActivityLogService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankAccountServiceImpl implements BankAccountService {

    private final BankAccountRepository repo;
    private final MessageService messageService;
    private final TenantContext tenantContext;
    private final AuthContext authContext;
    private final ActivityLogService activityLogService;

    @Override
    public List<BankAccountDTO> getAll() {
        return repo.findAllActive().stream().map(this::toDTO).toList();
    }

    @Override
    public BankAccountDTO getDefault() {
        return repo.findDefault().map(this::toDTO).orElse(null);
    }

    @Override
    @Transactional
    public BankAccountDTO create(SaveBankAccountRequest req) {
        BankAccount account = BankAccount.builder()
                .tenantId(tenantContext.getCurrentTenantId())
                .bankBin(req.getBankBin())
                .bankCode(req.getBankCode())
                .bankName(req.getBankName())
                .bankShortName(req.getBankShortName())
                .accountNumber(req.getAccountNumber())
                .accountName(req.getAccountName())
                .isDefault(Boolean.TRUE.equals(req.getIsDefault()))
                .build();
        if (Boolean.TRUE.equals(req.getIsDefault())) {
            repo.clearOtherDefaults(-1L);
        }
        BankAccount saved = repo.save(account);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.BANK_ACCOUNT_CREATED, "BANK_ACCOUNT", String.valueOf(saved.getId()),
                "Tạo tài khoản ngân hàng", null);
        return toDTO(saved);
    }

    @Override
    @Transactional
    public BankAccountDTO update(Long id, SaveBankAccountRequest req) {
        BankAccount account = findActive(id);
        account.setBankBin(req.getBankBin());
        account.setBankCode(req.getBankCode());
        account.setBankName(req.getBankName());
        account.setBankShortName(req.getBankShortName());
        account.setAccountNumber(req.getAccountNumber());
        account.setAccountName(req.getAccountName());
        if (Boolean.TRUE.equals(req.getIsDefault())) {
            repo.clearOtherDefaults(id);
            account.setIsDefault(true);
        } else {
            account.setIsDefault(false);
        }
        BankAccount saved = repo.save(account);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.BANK_ACCOUNT_UPDATED, "BANK_ACCOUNT", String.valueOf(saved.getId()),
                "Cập nhật tài khoản ngân hàng", null);
        return toDTO(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        BankAccount account = findActive(id);
        account.softDelete();
        repo.save(account);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.BANK_ACCOUNT_DELETED, "BANK_ACCOUNT", String.valueOf(id),
                "Xóa tài khoản ngân hàng", null);
    }

    @Override
    @Transactional
    public BankAccountDTO setDefault(Long id) {
        BankAccount account = findActive(id);
        repo.clearOtherDefaults(id);
        account.setIsDefault(true);
        BankAccount saved = repo.save(account);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.BANK_ACCOUNT_SET_DEFAULT, "BANK_ACCOUNT", String.valueOf(saved.getId()),
                "Đặt tài khoản ngân hàng mặc định", null);
        return toDTO(saved);
    }

    private BankAccount findActive(Long id) {
        BankAccount account = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(messageService.getMessage("error.bankAccount.notFound", new Object[]{id})));
        if (account.isDeleted()) throw new EntityNotFoundException(messageService.getMessage("error.bankAccount.notFound", new Object[]{id}));
        return account;
    }

    private BankAccountDTO toDTO(BankAccount b) {
        return BankAccountDTO.builder()
                .id(b.getId())
                .bankBin(b.getBankBin())
                .bankCode(b.getBankCode())
                .bankName(b.getBankName())
                .bankShortName(b.getBankShortName())
                .accountNumber(b.getAccountNumber())
                .accountName(b.getAccountName())
                .isDefault(b.getIsDefault())
                .build();
    }
}
