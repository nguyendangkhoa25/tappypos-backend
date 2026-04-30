package com.knp.service.vendor;

import com.knp.exception.BadRequestException;
import com.knp.service.MessageService;
import com.knp.exception.ResourceNotFoundException;
import com.knp.model.dto.vendor.*;
import com.knp.model.entity.vendor.PurchaseOrder;
import com.knp.model.entity.vendor.PurchaseOrderItem;
import com.knp.model.entity.vendor.Vendor;
import com.knp.repository.vendor.PurchaseOrderItemRepository;
import com.knp.repository.vendor.PurchaseOrderRepository;
import com.knp.repository.vendor.VendorRepository;
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
import com.knp.service.inventory.InventoryService;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseOrderService {

    private final PurchaseOrderRepository poRepository;
    private final PurchaseOrderItemRepository poItemRepository;
    private final VendorRepository vendorRepository;
    private final InventoryService inventoryService;
    private final MessageService messageService;

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

        List<PurchaseOrderItem> items = req.getItems().stream().map(i -> {
            PurchaseOrderItem item = PurchaseOrderItem.builder()
                    .purchaseOrder(po)
                    .productId(i.getProductId())
                    .productName(i.getProductName())
                    .productSku(i.getProductSku())
                    .quantityOrdered(i.getQuantityOrdered())
                    .quantityReceived(0)
                    .unitCost(i.getUnitCost())
                    .totalCost(i.getUnitCost().multiply(BigDecimal.valueOf(i.getQuantityOrdered())))
                    .build();
            return item;
        }).collect(Collectors.toList());

        po.setItems(items);
        po.recalculateTotal();

        PurchaseOrder saved = poRepository.save(po);
        log.info("Created purchase order {} for vendor {}", poNumber, vendor.getName());
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

            // Add to inventory
            if (item.getProductId() != null) {
                try {
                    var inventoryPage = inventoryService.getInventoryByProductId(
                            item.getProductId(), PageRequest.of(0, 1));
                    if (!inventoryPage.isEmpty()) {
                        inventoryService.addStock(inventoryPage.getContent().get(0).getId(), (long) qty);
                        log.info("Added {} units of product {} to inventory via PO {}", qty, item.getProductId(), po.getPoNumber());
                    } else {
                        log.warn("No inventory record found for product {} — stock not updated", item.getProductId());
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

        return mapToDTOWithItems(poRepository.save(po));
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
                .map(i -> PurchaseOrderItemDTO.builder()
                        .id(i.getId())
                        .productId(i.getProductId())
                        .productName(i.getProductName())
                        .productSku(i.getProductSku())
                        .quantityOrdered(i.getQuantityOrdered())
                        .quantityReceived(i.getQuantityReceived())
                        .unitCost(i.getUnitCost())
                        .totalCost(i.getTotalCost())
                        .build())
                .collect(Collectors.toList()));
        return dto;
    }
}
