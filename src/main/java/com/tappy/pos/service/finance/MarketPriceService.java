package com.tappy.pos.service.finance;

import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.marketprice.MarketPriceDTO;
import com.tappy.pos.model.dto.marketprice.SaveMarketPriceRequest;
import com.tappy.pos.model.entity.finance.MarketPrice;
import com.tappy.pos.repository.finance.MarketPriceRepository;
import com.tappy.pos.multitenant.TenantContext;
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
public class MarketPriceService {

    private final MarketPriceRepository repository;
    private final TenantContext tenantContext;

    public List<MarketPriceDTO> getAll() {
        return repository.findAllActive().stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional
    public MarketPriceDTO create(SaveMarketPriceRequest req) {
        MarketPrice mp = MarketPrice.builder()
                .tenantId(tenantContext.getCurrentTenantId())
                .name(req.getName()).unit(req.getUnit())
                .buyPrice(req.getBuyPrice()).sellPrice(req.getSellPrice())
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .notes(req.getNotes())
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 999)
                .build();
        return mapToDTO(repository.save(mp));
    }

    @Transactional
    public MarketPriceDTO update(Long id, SaveMarketPriceRequest req) {
        MarketPrice mp = findActive(id);
        mp.setName(req.getName()); mp.setUnit(req.getUnit());
        mp.setBuyPrice(req.getBuyPrice()); mp.setSellPrice(req.getSellPrice());
        if (req.getIsActive() != null) mp.setIsActive(req.getIsActive());
        mp.setNotes(req.getNotes());
        if (req.getSortOrder() != null) mp.setSortOrder(req.getSortOrder());
        return mapToDTO(repository.save(mp));
    }

    @Transactional
    public void delete(Long id) {
        MarketPrice mp = findActive(id);
        mp.softDelete();
        repository.save(mp);
    }

    private MarketPrice findActive(Long id) {
        return repository.findById(id)
                .filter(m -> !m.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Market price not found: " + id));
    }

    private MarketPriceDTO mapToDTO(MarketPrice m) {
        return MarketPriceDTO.builder()
                .id(m.getId()).name(m.getName()).unit(m.getUnit())
                .buyPrice(m.getBuyPrice()).sellPrice(m.getSellPrice())
                .isActive(m.getIsActive()).notes(m.getNotes())
                .sortOrder(m.getSortOrder()).updatedAt(m.getUpdatedAt())
                .build();
    }
}
