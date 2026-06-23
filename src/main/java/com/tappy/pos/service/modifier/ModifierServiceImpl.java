package com.tappy.pos.service.modifier;

import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.modifier.ModifierGroupDTO;
import com.tappy.pos.model.dto.modifier.SaveModifierGroupRequest;
import com.tappy.pos.model.entity.modifier.ModifierGroup;
import com.tappy.pos.model.entity.modifier.ModifierOption;
import com.tappy.pos.model.entity.modifier.ProductModifierGroup;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.config.AuthContext;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.repository.modifier.ModifierGroupRepository;
import com.tappy.pos.repository.modifier.ProductModifierGroupRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
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
    private final com.tappy.pos.repository.modifier.ModifierOptionRepository modifierOptionRepository;
    private final ProductModifierGroupRepository productModifierGroupRepository;
    private final TenantContext tenantContext;
    private final MessageService messageService;
    private final ActivityLogService activityLogService;
    private final AuthContext authContext;

    @Override
    @Transactional(readOnly = true)
    public List<ModifierGroupDTO> listGroups() {
        return modifierGroupRepository.findByDeletedFalseOrderBySortOrderAscIdAsc()
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.tappy.pos.model.dto.modifier.ChosenModifierDTO> resolveOptions(List<Long> optionIds) {
        if (optionIds == null || optionIds.isEmpty()) return List.of();
        var found = modifierOptionRepository.findAllByIdInWithGroup(optionIds).stream()
                .collect(Collectors.toMap(o -> o.getId(), o -> o, (a, b) -> a));
        // Preserve the caller's selection order; skip ids that don't resolve.
        return optionIds.stream()
                .map(found::get)
                .filter(java.util.Objects::nonNull)
                .map(o -> com.tappy.pos.model.dto.modifier.ChosenModifierDTO.builder()
                        .groupName(o.getModifierGroup() != null ? o.getModifierGroup().getName() : null)
                        .optionName(o.getName())
                        .priceDelta(o.getPriceDelta() != null ? o.getPriceDelta() : BigDecimal.ZERO)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public ModifierGroupDTO createGroup(SaveModifierGroupRequest req) {
        ModifierGroup group = new ModifierGroup();
        group.setTenantId(tenantContext.getCurrentTenantId());
        applyGroupFields(group, req);
        ModifierGroup saved = modifierGroupRepository.save(group);
        log.info("Created modifier group {} with {} options", saved.getId(), saved.getOptions().size());
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.MODIFIER_GROUP_CREATED, "MODIFIER_GROUP", String.valueOf(saved.getId()),
                "activity.modifier.group.created", null);
        return mapToDTO(saved);
    }

    @Override
    public ModifierGroupDTO updateGroup(Long id, SaveModifierGroupRequest req) {
        ModifierGroup group = requireGroup(id);
        group.getOptions().clear(); // orphanRemoval deletes the old options
        applyGroupFields(group, req);
        ModifierGroup saved = modifierGroupRepository.save(group);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.MODIFIER_GROUP_UPDATED, "MODIFIER_GROUP", String.valueOf(id),
                "activity.modifier.group.updated", null);
        return mapToDTO(saved);
    }

    @Override
    public void deleteGroup(Long id) {
        ModifierGroup group = requireGroup(id);
        group.softDelete();
        modifierGroupRepository.save(group);
        log.info("Soft-deleted modifier group {}", id);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.MODIFIER_GROUP_DELETED, "MODIFIER_GROUP", String.valueOf(id),
                "activity.modifier.group.deleted", null);
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
    @Transactional(readOnly = true)
    public java.util.Map<Long, List<ModifierGroupDTO>> getGroupsForProducts(java.util.Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return java.util.Map.of();
        List<ProductModifierGroup> links =
                productModifierGroupRepository.findByProductIdInOrderByProductIdAscSortOrderAscIdAsc(productIds);
        if (links.isEmpty()) return java.util.Map.of();

        // Load every referenced group once, map to DTO, then fan out per product preserving link order.
        java.util.Set<Long> groupIds = links.stream()
                .map(ProductModifierGroup::getModifierGroupId).collect(Collectors.toSet());
        java.util.Map<Long, ModifierGroupDTO> dtoById = modifierGroupRepository.findByIdInAndDeletedFalse(groupIds)
                .stream().map(this::mapToDTO)
                .collect(Collectors.toMap(ModifierGroupDTO::getId, g -> g, (a, b) -> a));

        java.util.Map<Long, List<ModifierGroupDTO>> result = new java.util.LinkedHashMap<>();
        for (ProductModifierGroup link : links) {
            ModifierGroupDTO dto = dtoById.get(link.getModifierGroupId());
            if (dto != null) {
                result.computeIfAbsent(link.getProductId(), k -> new ArrayList<>()).add(dto);
            }
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
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.MODIFIER_GROUP_PRODUCT_SET, "MODIFIER_GROUP", String.valueOf(productId),
                "activity.modifier.group.product.set", null);
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
