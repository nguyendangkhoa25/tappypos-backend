package com.tappy.pos.service.vehicle;

import com.tappy.pos.config.AuthContext;
import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.vehicle.CreateVehicleUnitRequest;
import com.tappy.pos.model.dto.vehicle.SellVehicleUnitRequest;
import com.tappy.pos.model.dto.vehicle.UpdateVehicleUnitRequest;
import com.tappy.pos.model.dto.vehicle.VehicleUnitDTO;
import com.tappy.pos.model.entity.product.Product;
import com.tappy.pos.model.entity.vehicle.VehicleUnitEntity;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.model.enums.VehicleUnitStatus;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.product.ProductRepository;
import com.tappy.pos.repository.vehicle.VehicleUnitRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Per-unit vehicle registry. Gated by the PRODUCT feature (a sub-capability of product —
 * no new flag). Enforces frame/engine uniqueness, supports "tra cứu xe theo số khung", and
 * flips a unit to SOLD with a computed bảo hành at checkout. See VEHICLE_SHOP_SHOP_TYPE_PLAN §4b.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VehicleUnitServiceImpl implements VehicleUnitService {

    private final VehicleUnitRepository vehicleUnitRepository;
    private final ProductRepository productRepository;
    private final TenantContext tenantContext;
    private final AuthContext authContext;
    private final ActivityLogService activityLogService;
    private final MessageService messageService;

    @Override
    public VehicleUnitDTO create(CreateVehicleUnitRequest request) {
        String tid = tenantContext.getCurrentTenantId();
        String actor = authContext.getCurrentUsername();
        LocalDateTime now = LocalDateTime.now();

        Product product = productRepository.findByIdAndDeletedFalse(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.product.not.found")));

        assertFrameEngineUnique(tid, request.getFrameNo(), request.getEngineNo());

        VehicleUnitEntity entity = VehicleUnitEntity.builder()
                .tenantId(tid)
                .productId(product.getId())
                .frameNo(trimToNull(request.getFrameNo()))
                .engineNo(trimToNull(request.getEngineNo()))
                .licensePlate(trimToNull(request.getLicensePlate()))
                .color(request.getColor())
                .odometerKm(request.getOdometerKm())
                .purchasePrice(request.getPurchasePrice())
                .currentValue(request.getCurrentValue())
                .status(VehicleUnitStatus.IN_STOCK)
                .conditionGrade(request.getConditionGrade())
                .warrantyMonths(request.getWarrantyMonths())
                .paperworkStatus(request.getPaperworkStatus())
                .notes(request.getNotes())
                .legacyId(request.getLegacyId())
                .createdBy(actor)
                .createdAt(now)
                .updatedAt(now)
                .deleted(false)
                .build();

        VehicleUnitEntity saved = vehicleUnitRepository.save(entity);
        activityLogService.logAsync(tid, actor, null,
                ActivityAction.VEHICLE_UNIT_CREATED, "VEHICLE_UNIT", String.valueOf(saved.getId()),
                "Thêm xe vào kho: " + product.getName()
                        + (saved.getFrameNo() != null ? " (số khung " + saved.getFrameNo() + ")" : ""), null);
        return toDTO(saved, product);
    }

    @Override
    public VehicleUnitDTO update(Long id, UpdateVehicleUnitRequest request) {
        String tid = tenantContext.getCurrentTenantId();
        VehicleUnitEntity entity = load(id);
        // Re-check uniqueness only if the number actually changed.
        if (request.getFrameNo() != null && !request.getFrameNo().equals(entity.getFrameNo())
                && vehicleUnitRepository.existsByFrameNoAndTenantIdAndDeletedFalse(request.getFrameNo().trim(), tid)) {
            throw new BadRequestException(messageService.getMessage("error.vehicle_unit.frameDuplicate"));
        }
        if (request.getEngineNo() != null && !request.getEngineNo().equals(entity.getEngineNo())
                && vehicleUnitRepository.existsByEngineNoAndTenantIdAndDeletedFalse(request.getEngineNo().trim(), tid)) {
            throw new BadRequestException(messageService.getMessage("error.vehicle_unit.engineDuplicate"));
        }
        if (request.getFrameNo() != null)        entity.setFrameNo(trimToNull(request.getFrameNo()));
        if (request.getEngineNo() != null)       entity.setEngineNo(trimToNull(request.getEngineNo()));
        if (request.getLicensePlate() != null)   entity.setLicensePlate(trimToNull(request.getLicensePlate()));
        if (request.getColor() != null)          entity.setColor(request.getColor());
        if (request.getOdometerKm() != null)     entity.setOdometerKm(request.getOdometerKm());
        if (request.getPurchasePrice() != null)  entity.setPurchasePrice(request.getPurchasePrice());
        if (request.getCurrentValue() != null)   entity.setCurrentValue(request.getCurrentValue());
        if (request.getConditionGrade() != null) entity.setConditionGrade(request.getConditionGrade());
        if (request.getWarrantyMonths() != null) entity.setWarrantyMonths(request.getWarrantyMonths());
        if (request.getPaperworkStatus() != null) entity.setPaperworkStatus(request.getPaperworkStatus());
        if (request.getNotes() != null)          entity.setNotes(request.getNotes());
        // Allow non-sale status transitions (RESERVED / DAMAGED / back to IN_STOCK); SOLD is set via markSold.
        if (request.getStatus() != null && request.getStatus() != VehicleUnitStatus.SOLD) {
            entity.setStatus(request.getStatus());
        }
        entity.setUpdatedAt(LocalDateTime.now());
        VehicleUnitEntity saved = vehicleUnitRepository.save(entity);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.VEHICLE_UNIT_UPDATED, "VEHICLE_UNIT", String.valueOf(id),
                "Cập nhật xe #" + id, null);
        return toDTO(saved, null);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleUnitDTO getById(Long id) {
        return toDTO(load(id), null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehicleUnitDTO> search(VehicleUnitStatus status, Long productId, Pageable pageable) {
        return vehicleUnitRepository.search(status, productId, pageable).map(v -> toDTO(v, null));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleUnitDTO> lookup(String keyword) {
        if (StringUtils.isBlank(keyword)) return List.of();
        return vehicleUnitRepository.lookup(keyword.trim()).stream().map(v -> toDTO(v, null)).toList();
    }

    @Override
    public VehicleUnitDTO markSold(Long id, SellVehicleUnitRequest request) {
        VehicleUnitEntity entity = load(id);
        if (entity.getStatus() == VehicleUnitStatus.SOLD) {
            throw new BadRequestException(messageService.getMessage("error.vehicle_unit.alreadySold"));
        }
        LocalDateTime now = LocalDateTime.now();
        entity.setStatus(VehicleUnitStatus.SOLD);
        entity.setSoldTo(request.getCustomerId());
        entity.setSoldToName(request.getCustomerName());
        entity.setSoldOrderId(request.getOrderId());
        entity.setSoldDate(now);
        Integer months = request.getWarrantyMonths() != null ? request.getWarrantyMonths() : entity.getWarrantyMonths();
        if (months != null && months > 0) {
            entity.setWarrantyMonths(months);
            entity.setWarrantyExp(LocalDate.now().plusMonths(months));
        }
        if (request.getPaperworkStatus() != null) entity.setPaperworkStatus(request.getPaperworkStatus());
        entity.setUpdatedAt(now);
        VehicleUnitEntity saved = vehicleUnitRepository.save(entity);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.VEHICLE_UNIT_SOLD, "VEHICLE_UNIT", String.valueOf(id),
                "Bán xe #" + id + (saved.getFrameNo() != null ? " (số khung " + saved.getFrameNo() + ")" : ""), null);
        return toDTO(saved, null);
    }

    @Override
    public void delete(Long id) {
        VehicleUnitEntity entity = load(id);
        if (entity.getStatus() == VehicleUnitStatus.SOLD) {
            throw new BadRequestException(messageService.getMessage("error.vehicle_unit.alreadySold"));
        }
        entity.setDeleted(true);
        entity.setDeletedAt(LocalDateTime.now());
        vehicleUnitRepository.save(entity);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.VEHICLE_UNIT_DELETED, "VEHICLE_UNIT", String.valueOf(id),
                "Xóa xe #" + id, null);
    }

    // ── helpers ──────────────────────────────────────────────────────────────
    private VehicleUnitEntity load(Long id) {
        return vehicleUnitRepository.findByIdAndDeletedFalse(id).orElseThrow(ResourceNotFoundException::new);
    }

    private void assertFrameEngineUnique(String tid, String frameNo, String engineNo) {
        if (StringUtils.isNotBlank(frameNo)
                && vehicleUnitRepository.existsByFrameNoAndTenantIdAndDeletedFalse(frameNo.trim(), tid)) {
            throw new BadRequestException(messageService.getMessage("error.vehicle_unit.frameDuplicate"));
        }
        if (StringUtils.isNotBlank(engineNo)
                && vehicleUnitRepository.existsByEngineNoAndTenantIdAndDeletedFalse(engineNo.trim(), tid)) {
            throw new BadRequestException(messageService.getMessage("error.vehicle_unit.engineDuplicate"));
        }
    }

    private static String trimToNull(String v) {
        return StringUtils.isBlank(v) ? null : v.trim();
    }

    private VehicleUnitDTO toDTO(VehicleUnitEntity e, Product product) {
        Product p = product != null ? product
                : productRepository.findByIdAndDeletedFalse(e.getProductId()).orElse(null);
        Boolean warrantyActive = e.getWarrantyExp() == null ? null
                : !e.getWarrantyExp().isBefore(LocalDate.now());
        return VehicleUnitDTO.builder()
                .id(e.getId())
                .productId(e.getProductId())
                .productName(p != null ? p.getName() : null)
                .productSku(p != null ? p.getSku() : null)
                .frameNo(e.getFrameNo())
                .engineNo(e.getEngineNo())
                .licensePlate(e.getLicensePlate())
                .color(e.getColor())
                .odometerKm(e.getOdometerKm())
                .purchasePrice(e.getPurchasePrice())
                .currentValue(e.getCurrentValue())
                .status(e.getStatus())
                .conditionGrade(e.getConditionGrade())
                .warrantyMonths(e.getWarrantyMonths())
                .warrantyExp(e.getWarrantyExp())
                .warrantyActive(warrantyActive)
                .paperworkStatus(e.getPaperworkStatus())
                .soldTo(e.getSoldTo())
                .soldToName(e.getSoldToName())
                .soldOrderId(e.getSoldOrderId())
                .soldDate(e.getSoldDate())
                .notes(e.getNotes())
                .createdBy(e.getCreatedBy())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
