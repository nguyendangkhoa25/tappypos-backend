package com.tappy.pos.service.finance;

import com.tappy.pos.model.dto.bank.BankDTO;

import java.util.List;

public interface BankService {
    List<BankDTO> getAllBanks();
}
