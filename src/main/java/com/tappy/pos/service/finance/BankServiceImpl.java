package com.tappy.pos.service.finance;

import com.tappy.pos.model.dto.bank.BankDTO;
import com.tappy.pos.repository.finance.BankRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankServiceImpl implements BankService {

    private final BankRepository bankRepository;

    @Override
    public List<BankDTO> getAllBanks() {
        return bankRepository.findAllActiveOrderBySortOrder()
                .stream()
                .map(b -> BankDTO.builder()
                        .id(b.getId())
                        .code(b.getCode())
                        .bin(b.getBin())
                        .name(b.getName())
                        .shortName(b.getShortName())
                        .sortOrder(b.getSortOrder())
                        .isActive(b.getIsActive())
                        .build())
                .collect(Collectors.toList());
    }
}
