package com.knp.service;

import com.knp.model.dto.bank.BankDTO;

import java.util.List;

public interface BankService {
    List<BankDTO> getAllBanks();
}
