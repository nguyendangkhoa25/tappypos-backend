package com.knp.service.finance;

import com.knp.model.dto.bank.BankAccountDTO;
import com.knp.model.dto.bank.SaveBankAccountRequest;
import com.knp.model.entity.finance.BankAccount;
import com.knp.repository.finance.BankAccountRepository;
import com.knp.service.MessageService;
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
        return toDTO(repo.save(account));
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
        return toDTO(repo.save(account));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        BankAccount account = findActive(id);
        account.softDelete();
        repo.save(account);
    }

    @Override
    @Transactional
    public BankAccountDTO setDefault(Long id) {
        BankAccount account = findActive(id);
        repo.clearOtherDefaults(id);
        account.setIsDefault(true);
        return toDTO(repo.save(account));
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
