package com.tappy.pos.service.stocktake;

import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.inventory.AdjustInventoryRequest;
import com.tappy.pos.model.dto.stocktake.*;
import com.tappy.pos.model.entity.inventory.Inventory;
import com.tappy.pos.model.entity.product.Product;
import com.tappy.pos.model.entity.stocktake.StocktakeCountEntity;
import com.tappy.pos.model.entity.stocktake.StocktakeSessionEntity;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.model.enums.StocktakeStatus;
import com.tappy.pos.repository.inventory.InventoryRepository;
import com.tappy.pos.repository.product.ProductRepository;
import com.tappy.pos.repository.stocktake.StocktakeCountRepository;
import com.tappy.pos.repository.stocktake.StocktakeSessionRepository;
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.inventory.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StocktakeServiceImpl implements StocktakeService {

    private final StocktakeSessionRepository sessionRepository;
    private final StocktakeCountRepository countRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final ActivityLogService activityLogService;
    private final TenantContext tenantContext;
    private final MessageService messageService;

    // ── Sessions ────────────────────────────────────────────────────────────────

    @Override
    public StocktakeSessionDTO createSession(CreateStocktakeSessionRequest request) {
        // Resume an existing open session rather than allowing two at once.
        Optional<StocktakeSessionEntity> open =
                sessionRepository.findFirstByStatusAndDeletedFalseOrderByStartedAtDesc(StocktakeStatus.IN_PROGRESS);
        if (open.isPresent()) {
            log.info("Returning existing open stocktake session: {}", open.get().getId());
            return toSessionDTO(open.get(), false);
        }

        String actor = currentUser();
        StocktakeSessionEntity session = StocktakeSessionEntity.builder()
                .tenantId(tenantContext.getCurrentTenantId())
                .name(request.getName())
                .note(request.getNote())
                .status(StocktakeStatus.IN_PROGRESS)
                .startedBy(actor)
                .startedAt(LocalDateTime.now())
                .build();
        session = sessionRepository.save(session);

        activityLogService.logAsync(tenantContext.getCurrentTenantId(), actor, null,
                ActivityAction.STOCKTAKE_STARTED, "STOCKTAKE", String.valueOf(session.getId()),
                "Bắt đầu kiểm kho" + (session.getName() != null ? ": " + session.getName() : ""), null);

        return toSessionDTO(session, false);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StocktakeSessionDTO> listSessions(StocktakeStatus status, Pageable pageable) {
        Page<StocktakeSessionEntity> page = (status == null)
                ? sessionRepository.findByDeletedFalseOrderByCreatedAtDesc(pageable)
                : sessionRepository.findByStatusAndDeletedFalseOrderByCreatedAtDesc(status, pageable);
        return page.map(s -> toSessionDTO(s, false));
    }

    @Override
    @Transactional(readOnly = true)
    public StocktakeSessionDTO getSession(Long sessionId) {
        StocktakeSessionEntity session = requireSession(sessionId);
        return toSessionDTO(session, true);
    }

    @Override
    @Transactional(readOnly = true)
    public StocktakeSessionDTO getActiveSession() {
        return sessionRepository.findFirstByStatusAndDeletedFalseOrderByStartedAtDesc(StocktakeStatus.IN_PROGRESS)
                .map(s -> toSessionDTO(s, false))
                .orElse(null);
    }

    // ── Counting ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public StocktakeProductLineDTO lookup(Long sessionId, String barcode) {
        requireSession(sessionId);
        if (barcode == null || barcode.isBlank()) {
            throw new BadRequestException(messageService.getMessage("error.stocktake.barcode.required"));
        }
        Product product = productRepository.findByBarcodeAndDeletedFalse(barcode.trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.stocktake.product.not.found", barcode)));

        Inventory inv = inventoryRepository.findProductLevelInventory(product.getId()).orElse(null);
        Long alreadyCounted = countRepository
                .findBySessionIdAndProductIdAndDeletedFalse(sessionId, product.getId())
                .map(StocktakeCountEntity::getCountedQty)
                .orElse(null);

        return StocktakeProductLineDTO.builder()
                .productId(product.getId())
                .productName(product.getName())
                .sku(product.getSku())
                .barcode(product.getBarcode())
                .expectedQty(inv != null ? inv.getQuantityInStock() : 0L)
                .alreadyCountedQty(alreadyCounted)
                .build();
    }

    @Override
    public StocktakeCountDTO upsertCount(Long sessionId, UpsertCountRequest request) {
        StocktakeSessionEntity session = requireSession(sessionId);
        requireInProgress(session);

        Product product = resolveProduct(request);
        Inventory inv = inventoryRepository.findProductLevelInventory(product.getId()).orElse(null);

        String actor = currentUser();
        StocktakeCountEntity count = countRepository
                .findBySessionIdAndProductIdAndDeletedFalse(sessionId, product.getId())
                .orElse(null);

        if (count == null) {
            // First count for this product — snapshot the expected (system) quantity now.
            long expected = inv != null ? inv.getQuantityInStock() : 0L;
            count = StocktakeCountEntity.builder()
                    .tenantId(tenantContext.getCurrentTenantId())
                    .sessionId(sessionId)
                    .productId(product.getId())
                    .inventoryId(inv != null ? inv.getId() : null)
                    .expectedQty(expected)
                    .countedQty(request.getCountedQty())
                    .difference(request.getCountedQty() - expected)
                    .build();
        } else {
            count.setCountedQty(request.getCountedQty());
            count.setDifference(request.getCountedQty() - count.getExpectedQty());
        }
        count.setCountedBy(actor);
        count.setCountedAt(LocalDateTime.now());
        if (request.getNote() != null) count.setNote(request.getNote());
        count = countRepository.save(count);

        return toCountDTO(count, product);
    }

    @Override
    public void deleteCount(Long sessionId, Long countId) {
        StocktakeSessionEntity session = requireSession(sessionId);
        requireInProgress(session);
        StocktakeCountEntity count = countRepository.findByIdAndDeletedFalse(countId)
                .filter(c -> c.getSessionId().equals(sessionId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.stocktake.count.not.found")));
        count.softDelete();
        countRepository.save(count);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StocktakeCountDTO> getDiscrepancies(Long sessionId) {
        requireSession(sessionId);
        return toCountDTOs(countRepository.findDiscrepancies(sessionId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StocktakeProductLineDTO> getUncounted(Long sessionId) {
        requireSession(sessionId);
        List<Long> counted = countRepository.findCountedProductIds(sessionId);
        List<Inventory> uncounted = counted.isEmpty()
                ? inventoryRepository.findAllProductLevelActive()
                : inventoryRepository.findProductLevelActiveExcluding(counted);
        return uncounted.stream().map(inv -> {
            Product p = inv.getProduct();
            return StocktakeProductLineDTO.builder()
                    .productId(p != null ? p.getId() : null)
                    .productName(p != null ? p.getName() : null)
                    .sku(p != null ? p.getSku() : null)
                    .barcode(p != null ? p.getBarcode() : null)
                    .expectedQty(inv.getQuantityInStock())
                    .alreadyCountedQty(null)
                    .build();
        }).collect(Collectors.toList());
    }

    // ── Finalize ────────────────────────────────────────────────────────────────

    @Override
    public StocktakeSessionDTO apply(Long sessionId) {
        StocktakeSessionEntity session = requireSession(sessionId);
        requireInProgress(session);

        List<StocktakeCountEntity> counts = countRepository.findBySessionIdAndDeletedFalseOrderByCountedAtDesc(sessionId);
        int adjusted = 0;
        for (StocktakeCountEntity count : counts) {
            if (Boolean.TRUE.equals(count.getApplied())) continue;
            // Set stock to the absolute counted value: delta against CURRENT stock.
            Inventory inv = inventoryRepository.findProductLevelInventory(count.getProductId()).orElse(null);
            if (inv != null) {
                long current = inv.getQuantityInStock() != null ? inv.getQuantityInStock() : 0L;
                long delta = count.getCountedQty() - current;
                if (delta != 0) {
                    AdjustInventoryRequest adj = new AdjustInventoryRequest();
                    adj.setProductId(count.getProductId());
                    adj.setQuantity(delta);
                    adj.setReason("Kiểm kho #" + sessionId);
                    adj.setNote(count.getNote());
                    inventoryService.adjustByProductId(adj);
                    adjusted++;
                }
            }
            count.setApplied(true);
            countRepository.save(count);
        }

        String actor = currentUser();
        session.setStatus(StocktakeStatus.COMPLETED);
        session.setCompletedBy(actor);
        session.setCompletedAt(LocalDateTime.now());
        session = sessionRepository.save(session);

        activityLogService.logAsync(tenantContext.getCurrentTenantId(), actor, null,
                ActivityAction.STOCKTAKE_APPLIED, "STOCKTAKE", String.valueOf(sessionId),
                "Áp dụng kiểm kho: điều chỉnh " + adjusted + " sản phẩm", null);

        return toSessionDTO(session, true);
    }

    @Override
    public StocktakeSessionDTO cancel(Long sessionId) {
        StocktakeSessionEntity session = requireSession(sessionId);
        requireInProgress(session);
        String actor = currentUser();
        session.setStatus(StocktakeStatus.CANCELLED);
        session.setCompletedBy(actor);
        session.setCompletedAt(LocalDateTime.now());
        session = sessionRepository.save(session);

        activityLogService.logAsync(tenantContext.getCurrentTenantId(), actor, null,
                ActivityAction.STOCKTAKE_CANCELLED, "STOCKTAKE", String.valueOf(sessionId),
                "Hủy phiên kiểm kho", null);

        return toSessionDTO(session, false);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private StocktakeSessionEntity requireSession(Long sessionId) {
        return sessionRepository.findByIdAndDeletedFalse(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.stocktake.session.not.found")));
    }

    private void requireInProgress(StocktakeSessionEntity session) {
        if (session.getStatus() != StocktakeStatus.IN_PROGRESS) {
            throw new BadRequestException(messageService.getMessage("error.stocktake.session.not.active"));
        }
    }

    private Product resolveProduct(UpsertCountRequest request) {
        if (request.getProductId() != null) {
            return productRepository.findByIdAndDeletedFalse(request.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            messageService.getMessage("error.stocktake.product.not.found",
                                    String.valueOf(request.getProductId()))));
        }
        if (request.getBarcode() != null && !request.getBarcode().isBlank()) {
            return productRepository.findByBarcodeAndDeletedFalse(request.getBarcode().trim())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            messageService.getMessage("error.stocktake.product.not.found", request.getBarcode())));
        }
        throw new BadRequestException(messageService.getMessage("error.stocktake.product.required"));
    }

    private String currentUser() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return null;
        }
    }

    private StocktakeSessionDTO toSessionDTO(StocktakeSessionEntity s, boolean includeCounts) {
        long counted = countRepository.countBySessionIdAndDeletedFalse(s.getId());
        long discrepancies = countRepository.findDiscrepancies(s.getId()).size();
        StocktakeSessionDTO.StocktakeSessionDTOBuilder b = StocktakeSessionDTO.builder()
                .id(s.getId())
                .name(s.getName())
                .status(s.getStatus())
                .note(s.getNote())
                .startedBy(s.getStartedBy())
                .startedAt(s.getStartedAt())
                .completedBy(s.getCompletedBy())
                .completedAt(s.getCompletedAt())
                .createdAt(s.getCreatedAt())
                .countedItems(counted)
                .discrepancyCount(discrepancies);
        if (includeCounts) {
            b.counts(toCountDTOs(countRepository.findBySessionIdAndDeletedFalseOrderByCountedAtDesc(s.getId())));
        }
        return b.build();
    }

    /** Batch-map count entities, fetching their products in a single query to avoid N+1. */
    private List<StocktakeCountDTO> toCountDTOs(List<StocktakeCountEntity> counts) {
        if (counts.isEmpty()) return List.of();
        List<Long> productIds = counts.stream().map(StocktakeCountEntity::getProductId).distinct().collect(Collectors.toList());
        Map<Long, Product> products = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        return counts.stream()
                .map(c -> toCountDTO(c, products.get(c.getProductId())))
                .collect(Collectors.toList());
    }

    private StocktakeCountDTO toCountDTO(StocktakeCountEntity c, Product product) {
        return StocktakeCountDTO.builder()
                .id(c.getId())
                .sessionId(c.getSessionId())
                .productId(c.getProductId())
                .productName(product != null ? product.getName() : null)
                .sku(product != null ? product.getSku() : null)
                .barcode(product != null ? product.getBarcode() : null)
                .expectedQty(c.getExpectedQty())
                .countedQty(c.getCountedQty())
                .difference(c.getDifference())
                .countedBy(c.getCountedBy())
                .countedAt(c.getCountedAt())
                .applied(c.getApplied())
                .note(c.getNote())
                .build();
    }
}
