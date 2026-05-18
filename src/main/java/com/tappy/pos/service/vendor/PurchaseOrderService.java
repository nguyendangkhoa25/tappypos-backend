package com.tappy.pos.service.vendor;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.vendor.*;
import com.tappy.pos.model.entity.vendor.PurchaseOrder;
import com.tappy.pos.model.entity.vendor.PurchaseOrderItem;
import com.tappy.pos.model.entity.vendor.Vendor;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.repository.product.ProductVariantRepository;
import com.tappy.pos.repository.vendor.PurchaseOrderItemRepository;
import com.tappy.pos.repository.vendor.PurchaseOrderRepository;
import com.tappy.pos.repository.vendor.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.tappy.pos.service.inventory.InventoryService;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseOrderService {

    private final PurchaseOrderRepository poRepository;
    private final PurchaseOrderItemRepository poItemRepository;
    private final VendorRepository vendorRepository;
    private final InventoryService inventoryService;
    private final ProductVariantRepository productVariantRepository;
    private final MessageService messageService;
    private final TenantContext tenantContext;
    private final ActivityLogService activityLogService;

    public Page<PurchaseOrderDTO> getAll(String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            return poRepository.findByStatus(PurchaseOrder.PoStatus.valueOf(status.toUpperCase()), pageable)
                    .map(this::mapToDTO);
        }
        return poRepository.findAllActive(pageable).map(this::mapToDTO);
    }

    public PurchaseOrderDTO getById(Long id) {
        return findActive(id).map(this::mapToDTOWithItems)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.po.not.found", id)));
    }

    public Page<PurchaseOrderDTO> getByVendor(Long vendorId, Pageable pageable) {
        return poRepository.findByVendorId(vendorId, pageable).map(this::mapToDTO);
    }

    @Transactional
    public PurchaseOrderDTO create(CreatePurchaseOrderRequest req) {
        Vendor vendor = vendorRepository.findById(req.getVendorId())
                .filter(v -> !v.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.vendor.not.found", req.getVendorId())));

        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        String tenantId = tenantContext.getCurrentTenantId();
        String poNumber = generatePoNumber();

        PurchaseOrder po = PurchaseOrder.builder()
                .poNumber(poNumber)
                .vendor(vendor)
                .status(PurchaseOrder.PoStatus.DRAFT)
                .expectedDate(req.getExpectedDate())
                .notes(req.getNotes())
                .createdBy(currentUser)
                .totalAmount(BigDecimal.ZERO)
                .build();
        po.setTenantId(tenantId);

        List<PurchaseOrderItem> items = req.getItems().stream().map(i -> {
            PurchaseOrderItem item = PurchaseOrderItem.builder()
                    .purchaseOrder(po)
                    .productId(i.getProductId())
                    .variantId(i.getVariantId())
                    .productName(i.getProductName())
                    .productSku(i.getProductSku())
                    .quantityOrdered(i.getQuantityOrdered())
                    .quantityReceived(0)
                    .unitCost(i.getUnitCost())
                    .totalCost(i.getUnitCost().multiply(BigDecimal.valueOf(i.getQuantityOrdered())))
                    .build();
            item.setTenantId(tenantId);
            return item;
        }).collect(Collectors.toList());

        po.setItems(items);
        po.recalculateTotal();

        PurchaseOrder saved = poRepository.save(po);
        log.info("Created purchase order {} for vendor {}", poNumber, vendor.getName());

        activityLogService.logAsync(tenantContext.getCurrentTenantId(), currentUser, null,
                ActivityAction.PURCHASE_ORDER_CREATED, "PURCHASE_ORDER", saved.getPoNumber(),
                "Tạo đơn nhập hàng " + saved.getPoNumber() + " từ " + vendor.getName(), null);

        return mapToDTOWithItems(saved);
    }

    @Transactional
    public PurchaseOrderDTO submit(Long id) {
        PurchaseOrder po = findActive(id)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.po.not.found", id)));
        if (po.getStatus() != PurchaseOrder.PoStatus.DRAFT) {
            throw new BadRequestException(messageService.getMessage("error.po.submit.not.draft", po.getStatus()));
        }
        po.setStatus(PurchaseOrder.PoStatus.ORDERED);
        po.setOrderedAt(LocalDateTime.now());
        return mapToDTOWithItems(poRepository.save(po));
    }

    @Transactional
    public PurchaseOrderDTO receiveItems(Long id, ReceiveItemsRequest req) {
        PurchaseOrder po = findActive(id)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.po.not.found", id)));

        if (po.getStatus() == PurchaseOrder.PoStatus.DRAFT
                || po.getStatus() == PurchaseOrder.PoStatus.CANCELLED
                || po.getStatus() == PurchaseOrder.PoStatus.RECEIVED) {
            throw new BadRequestException(messageService.getMessage("error.po.receive.invalid.status", po.getStatus()));
        }

        Map<Long, Integer> receiveMap = req.getItems().stream()
                .collect(Collectors.toMap(
                        ReceiveItemsRequest.ItemReceive::getItemId,
                        ReceiveItemsRequest.ItemReceive::getQuantityReceived));

        for (PurchaseOrderItem item : po.getItems()) {
            Integer qty = receiveMap.get(item.getId());
            if (qty == null || qty <= 0) continue;

            int newReceived = item.getQuantityReceived() + qty;
            if (newReceived > item.getQuantityOrdered()) {
                throw new BadRequestException(messageService.getMessage(
                        "error.po.receive.exceeds.ordered",
                        item.getProductName(), item.getQuantityOrdered(), item.getQuantityReceived()));
            }
            item.setQuantityReceived(newReceived);

            // Add to inventory — variant-aware: if this line item specifies a variant,
            // add stock to the variant's inventory record; otherwise add to the product-level record.
            if (item.getProductId() != null) {
                try {
                    var pageable = PageRequest.of(0, 1);
                    var inventoryPage = (item.getVariantId() != null)
                            ? inventoryService.getInventoryByProductIdAndVariantId(item.getProductId(), item.getVariantId(), pageable)
                            : inventoryService.getInventoryByProductId(item.getProductId(), pageable);
                    if (!inventoryPage.isEmpty()) {
                        inventoryService.addStock(inventoryPage.getContent().get(0).getId(), (long) qty);
                        log.info("Added {} units of product {} (variant {}) to inventory via PO {}",
                                qty, item.getProductId(), item.getVariantId(), po.getPoNumber());
                    } else {
                        log.warn("No inventory record found for product {} variant {} — stock not updated",
                                item.getProductId(), item.getVariantId());
                    }
                } catch (Exception e) {
                    log.warn("Failed to update inventory for product {}: {}", item.getProductId(), e.getMessage());
                }
            }
        }

        // Update PO status
        boolean allReceived = po.getItems().stream()
                .allMatch(i -> i.getQuantityReceived() >= i.getQuantityOrdered());
        boolean anyReceived = po.getItems().stream()
                .anyMatch(i -> i.getQuantityReceived() > 0);

        if (allReceived) {
            po.setStatus(PurchaseOrder.PoStatus.RECEIVED);
            po.setReceivedAt(LocalDateTime.now());
        } else if (anyReceived) {
            po.setStatus(PurchaseOrder.PoStatus.PARTIALLY_RECEIVED);
        }

        PurchaseOrder saved = poRepository.save(po);
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), currentUser, null,
                ActivityAction.PURCHASE_ORDER_RECEIVED, "PURCHASE_ORDER", saved.getPoNumber(),
                "Nhận hàng đơn nhập " + saved.getPoNumber(), null);
        return mapToDTOWithItems(saved);
    }

    @Transactional
    public PurchaseOrderDTO cancel(Long id) {
        PurchaseOrder po = findActive(id)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.po.not.found", id)));
        if (po.getStatus() == PurchaseOrder.PoStatus.RECEIVED
                || po.getStatus() == PurchaseOrder.PoStatus.PARTIALLY_RECEIVED) {
            throw new BadRequestException(messageService.getMessage("error.po.cancel.invalid.status", po.getStatus()));
        }
        po.setStatus(PurchaseOrder.PoStatus.CANCELLED);
        return mapToDTOWithItems(poRepository.save(po));
    }

    @Transactional
    public void delete(Long id) {
        PurchaseOrder po = findActive(id)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.po.not.found", id)));
        if (po.getStatus() != PurchaseOrder.PoStatus.DRAFT) {
            throw new BadRequestException(messageService.getMessage("error.po.delete.not.draft"));
        }
        po.softDelete();
        poRepository.save(po);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private java.util.Optional<PurchaseOrder> findActive(Long id) {
        return poRepository.findById(id).filter(p -> !p.isDeleted());
    }

    private String generatePoNumber() {
        Integer maxSeq = poRepository.findMaxPoSequence();
        int next = (maxSeq != null ? maxSeq : 0) + 1;
        return String.format("PO-%06d", next);
    }

    private PurchaseOrderDTO mapToDTO(PurchaseOrder po) {
        return PurchaseOrderDTO.builder()
                .id(po.getId())
                .poNumber(po.getPoNumber())
                .vendorId(po.getVendor().getId())
                .vendorName(po.getVendor().getName())
                .vendorCode(po.getVendor().getCode())
                .status(po.getStatus().name())
                .totalAmount(po.getTotalAmount())
                .expectedDate(po.getExpectedDate())
                .orderedAt(po.getOrderedAt())
                .receivedAt(po.getReceivedAt())
                .createdBy(po.getCreatedBy())
                .notes(po.getNotes())
                .createdAt(po.getCreatedAt())
                .build();
    }

    private PurchaseOrderDTO mapToDTOWithItems(PurchaseOrder po) {
        PurchaseOrderDTO dto = mapToDTO(po);
        dto.setItems(po.getItems().stream()
                .filter(i -> !i.isDeleted())
                .map(i -> {
                    String variantLabel = null;
                    if (i.getVariantId() != null) {
                        variantLabel = productVariantRepository.findById(i.getVariantId())
                                .map(v -> v.getVariantOptions() != null
                                        ? String.join(" / ", v.getVariantOptions().values())
                                        : null)
                                .orElse(null);
                    }
                    return PurchaseOrderItemDTO.builder()
                            .id(i.getId())
                            .productId(i.getProductId())
                            .variantId(i.getVariantId())
                            .variantLabel(variantLabel)
                            .productName(i.getProductName())
                            .productSku(i.getProductSku())
                            .quantityOrdered(i.getQuantityOrdered())
                            .quantityReceived(i.getQuantityReceived())
                            .unitCost(i.getUnitCost())
                            .totalCost(i.getTotalCost())
                            .build();
                })
                .collect(Collectors.toList()));
        return dto;
    }
}
