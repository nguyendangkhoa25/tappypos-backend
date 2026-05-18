package com.tappy.pos.service.finance;

import com.tappy.pos.model.dto.bank.BankAccountDTO;
import com.tappy.pos.model.dto.bank.SaveBankAccountRequest;

import java.util.List;

public interface BankAccountService {
    List<BankAccountDTO> getAll();
    BankAccountDTO getDefault();
    BankAccountDTO create(SaveBankAccountRequest req);
    BankAccountDTO update(Long id, SaveBankAccountRequest req);
    void delete(Long id);
    BankAccountDTO setDefault(Long id);
}
