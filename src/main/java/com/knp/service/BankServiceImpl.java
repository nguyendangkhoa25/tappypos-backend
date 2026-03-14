package com.knp.service;

import com.knp.model.dto.bank.BankDTO;
import com.knp.model.entity.Tenant;
import com.knp.multitenant.TenantContext;
import com.knp.repository.BankRepository;
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
    private final TenantContext tenantContext;

    @Override
    public List<BankDTO> getAllBanks() {
        log.info("Getting all active banks from master DB");
        // Banks table lives in master DB — temporarily clear tenant context so
        // RoutingDataSource falls back to the master datasource.
        Tenant saved = tenantContext.getCurrentTenant();
        try {
            if (saved != null) tenantContext.clear();
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
        } finally {
            if (saved != null) tenantContext.setCurrentTenant(saved);
        }
    }
}
