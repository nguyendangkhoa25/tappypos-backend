package com.tappy.pos.service.modifier;

import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.modifier.ModifierGroupDTO;
import com.tappy.pos.model.dto.modifier.SaveModifierGroupRequest;
import com.tappy.pos.model.entity.modifier.ModifierGroup;
import com.tappy.pos.model.entity.modifier.ModifierOption;
import com.tappy.pos.model.entity.modifier.ProductModifierGroup;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.modifier.ModifierGroupRepository;
import com.tappy.pos.repository.modifier.ProductModifierGroupRepository;
import com.tappy.pos.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ModifierServiceImpl implements ModifierService {

    private final ModifierGroupRepository modifierGroupRepository;
    private final ProductModifierGroupRepository productModifierGroupRepository;
    private final TenantContext tenantContext;
    private final MessageService messageService;

    @Override
    @Transactional(readOnly = true)
    public List<ModifierGroupDTO> listGroups() {
        return modifierGroupRepository.findByDeletedFalseOrderBySortOrderAscIdAsc()
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public ModifierGroupDTO createGroup(SaveModifierGroupRequest req) {
        ModifierGroup group = new ModifierGroup();
        group.setTenantId(tenantContext.getCurrentTenantId());
        applyGroupFields(group, req);
        ModifierGroup saved = modifierGroupRepository.save(group);
        log.info("Created modifier group {} with {} options", saved.getId(), saved.getOptions().size());
        return mapToDTO(saved);
    }

    @Override
    public ModifierGroupDTO updateGroup(Long id, SaveModifierGroupRequest req) {
        ModifierGroup group = requireGroup(id);
        group.getOptions().clear(); // orphanRemoval deletes the old options
        applyGroupFields(group, req);
        return mapToDTO(modifierGroupRepository.save(group));
    }

    @Override
    public void deleteGroup(Long id) {
        ModifierGroup group = requireGroup(id);
        group.softDelete();
        modifierGroupRepository.save(group);
        log.info("Soft-deleted modifier group {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModifierGroupDTO> getGroupsForProduct(Long productId) {
        List<ProductModifierGroup> links = productModifierGroupRepository.findByProductIdOrderBySortOrderAscIdAsc(productId);
        List<ModifierGroupDTO> result = new ArrayList<>();
        for (ProductModifierGroup link : links) {
            modifierGroupRepository.findByIdAndDeletedFalse(link.getModifierGroupId())
                    .ifPresent(g -> result.add(mapToDTO(g)));
        }
        return result;
    }

    @Override
    public void setProductGroups(Long productId, List<Long> groupIds) {
        productModifierGroupRepository.deleteByProductId(productId);
        if (groupIds == null) return;
        String tenantId = tenantContext.getCurrentTenantId();
        int order = 0;
        for (Long groupId : groupIds) {
            // Skip ids that don't resolve to a live group for this tenant.
            if (modifierGroupRepository.findByIdAndDeletedFalse(groupId).isEmpty()) continue;
            ProductModifierGroup link = ProductModifierGroup.builder()
                    .tenantId(tenantId)
                    .productId(productId)
                    .modifierGroupId(groupId)
                    .sortOrder(order++)
                    .build();
            productModifierGroupRepository.save(link);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void applyGroupFields(ModifierGroup group, SaveModifierGroupRequest req) {
        group.setName(req.getName());
        group.setMinSelect(req.getMinSelect() != null ? req.getMinSelect() : 0);
        group.setMaxSelect(req.getMaxSelect() != null ? req.getMaxSelect() : 1);
        group.setRequired(Boolean.TRUE.equals(req.getRequired()));
        group.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
        if (req.getOptions() != null) {
            int order = 0;
            for (var opt : req.getOptions()) {
                ModifierOption option = ModifierOption.builder()
                        .tenantId(group.getTenantId())
                        .modifierGroup(group)
                        .name(opt.getName())
                        .priceDelta(opt.getPriceDelta() != null ? opt.getPriceDelta() : BigDecimal.ZERO)
                        .sortOrder(opt.getSortOrder() != null ? opt.getSortOrder() : order++)
                        .build();
                group.getOptions().add(option);
            }
        }
    }

    private ModifierGroup requireGroup(Long id) {
        return modifierGroupRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.modifier.group.not.found", id)));
    }

    private ModifierGroupDTO mapToDTO(ModifierGroup g) {
        List<ModifierGroupDTO.OptionDTO> options = g.getOptions().stream()
                .map(o -> ModifierGroupDTO.OptionDTO.builder()
                        .id(o.getId())
                        .name(o.getName())
                        .priceDelta(o.getPriceDelta())
                        .sortOrder(o.getSortOrder())
                        .build())
                .collect(Collectors.toList());
        return ModifierGroupDTO.builder()
                .id(g.getId())
                .name(g.getName())
                .minSelect(g.getMinSelect())
                .maxSelect(g.getMaxSelect())
                .required(g.getRequired())
                .sortOrder(g.getSortOrder())
                .options(options)
                .build();
    }
}
