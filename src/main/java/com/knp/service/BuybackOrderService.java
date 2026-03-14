package com.knp.service;

import com.knp.exception.BadRequestException;
import com.knp.exception.ResourceNotFoundException;
import com.knp.model.dto.buyback.BuybackOrderDTO;
import com.knp.model.dto.buyback.BuybackOrderItemDTO;
import com.knp.model.dto.buyback.CreateBuybackOrderRequest;
import com.knp.model.entity.BuybackOrder;
import com.knp.model.entity.BuybackOrder.OrderStatus;
import com.knp.model.entity.BuybackOrder.OrderType;
import com.knp.model.entity.BuybackOrderItem;
import com.knp.model.entity.BuybackOrderItem.ItemType;
import com.knp.model.entity.BuybackOrderItem.ItemCondition;
import com.knp.repository.BuybackOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BuybackOrderService {

    private final BuybackOrderRepository repository;

    public Page<BuybackOrderDTO> getAll(String type, String status, Pageable pageable) {
        OrderType  typeEnum   = parseType(type);
        OrderStatus statusEnum = parseStatus(status);

        if (typeEnum != null && statusEnum != null) {
            return repository.findByTypeAndStatus(typeEnum, statusEnum, pageable).map(this::mapToDTO);
        } else if (typeEnum != null) {
            return repository.findByType(typeEnum, pageable).map(this::mapToDTO);
        } else if (statusEnum != null) {
            return repository.findByStatus(statusEnum, pageable).map(this::mapToDTO);
        }
        return repository.findAllActive(pageable).map(this::mapToDTO);
    }

    public BuybackOrderDTO getById(Long id) {
        return mapToDTOWithItems(findActive(id));
    }

    @Transactional
    public BuybackOrderDTO create(CreateBuybackOrderRequest req) {
        String currentUser  = currentUsername();
        OrderType orderType = OrderType.valueOf(req.getType().toUpperCase());
        OrderStatus status  = req.getStatus() != null
                ? OrderStatus.valueOf(req.getStatus().toUpperCase())
                : OrderStatus.PENDING;

        BuybackOrder order = BuybackOrder.builder()
                .orderNumber(generateOrderNumber())
                .type(orderType)
                .status(status)
                .customerId(req.getCustomerId())
                .customerName(req.getCustomerName())
                .customerPhone(req.getCustomerPhone())
                .paymentMethod(req.getPaymentMethod())
                .buyTotal(nvl(req.getBuyTotal()))
                .saleTotal(nvl(req.getSaleTotal()))
                .netAmount(nvl(req.getNetAmount()))
                .notes(req.getNotes())
                .createdBy(currentUser)
                .build();

        if (status == OrderStatus.COMPLETED) {
            order.complete(currentUser);
        }

        List<BuybackOrderItem> items = new ArrayList<>();

        for (CreateBuybackOrderRequest.BuyItemRequest bi : req.getBuyItems()) {
            BuybackOrderItem item = BuybackOrderItem.builder()
                    .buybackOrder(order)
                    .itemType(ItemType.BUY)
                    .commodityId(bi.getCommodityId())
                    .commodityName(bi.getCommodityName())
                    .unit(bi.getUnit())
                    .weight(bi.getWeight())
                    .pricePerUnit(nvl(bi.getPricePerUnit()))
                    .totalPrice(nvl(bi.getTotalPrice()))
                    .conditionType(parseCondition(bi.getCondition()))
                    .notes(bi.getNotes())
                    .build();
            items.add(item);
        }

        if (orderType == OrderType.EXCHANGE && req.getSaleItems() != null) {
            for (CreateBuybackOrderRequest.SaleItemRequest si : req.getSaleItems()) {
                BuybackOrderItem item = BuybackOrderItem.builder()
                        .buybackOrder(order)
                        .itemType(ItemType.SALE)
                        .productName(si.getProductName())
                        .quantity(si.getQuantity())
                        .unitPrice(nvl(si.getUnitPrice()))
                        .totalPrice(nvl(si.getTotalPrice()))
                        .build();
                items.add(item);
            }
        }

        order.setItems(items);
        BuybackOrder saved = repository.save(order);
        log.info("Created buyback order {} (type={}, status={})", saved.getOrderNumber(), orderType, status);
        return mapToDTOWithItems(saved);
    }

    @Transactional
    public BuybackOrderDTO complete(Long id) {
        BuybackOrder order = findActive(id);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Only PENDING orders can be completed. Current: " + order.getStatus());
        }
        order.complete(currentUsername());
        return mapToDTOWithItems(repository.save(order));
    }

    @Transactional
    public BuybackOrderDTO cancel(Long id) {
        BuybackOrder order = findActive(id);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Order is already cancelled.");
        }
        order.cancel(currentUsername());
        return mapToDTOWithItems(repository.save(order));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private BuybackOrder findActive(Long id) {
        return repository.findById(id)
                .filter(o -> !o.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Buyback order not found: " + id));
    }

    private String generateOrderNumber() {
        Long maxId = repository.findMaxId();
        long next = (maxId != null ? maxId : 0L) + 1;
        return String.format("BB-%06d", next);
    }

    private String currentUsername() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "system";
        }
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private OrderType parseType(String t) {
        if (t == null || t.isBlank()) return null;
        try { return OrderType.valueOf(t.toUpperCase()); } catch (Exception e) { return null; }
    }

    private OrderStatus parseStatus(String s) {
        if (s == null || s.isBlank()) return null;
        try { return OrderStatus.valueOf(s.toUpperCase()); } catch (Exception e) { return null; }
    }

    private ItemCondition parseCondition(String c) {
        if (c == null || c.isBlank()) return ItemCondition.USED;
        try { return ItemCondition.valueOf(c.toUpperCase()); } catch (Exception e) { return ItemCondition.USED; }
    }

    private BuybackOrderDTO mapToDTO(BuybackOrder o) {
        return BuybackOrderDTO.builder()
                .id(o.getId())
                .orderNumber(o.getOrderNumber())
                .type(o.getType().name())
                .status(o.getStatus().name())
                .customerId(o.getCustomerId())
                .customerName(o.getCustomerName())
                .customerPhone(o.getCustomerPhone())
                .paymentMethod(o.getPaymentMethod())
                .buyTotal(o.getBuyTotal())
                .saleTotal(o.getSaleTotal())
                .netAmount(o.getNetAmount())
                .notes(o.getNotes())
                .createdBy(o.getCreatedBy())
                .completedAt(o.getCompletedAt())
                .cancelledAt(o.getCancelledAt())
                .createdAt(o.getCreatedAt())
                .build();
    }

    private BuybackOrderDTO mapToDTOWithItems(BuybackOrder o) {
        BuybackOrderDTO dto = mapToDTO(o);

        List<BuybackOrderItemDTO> buyItems = o.getItems().stream()
                .filter(i -> !i.isDeleted() && i.getItemType() == ItemType.BUY)
                .map(this::mapItemToDTO)
                .collect(Collectors.toList());

        List<BuybackOrderItemDTO> saleItems = o.getItems().stream()
                .filter(i -> !i.isDeleted() && i.getItemType() == ItemType.SALE)
                .map(this::mapItemToDTO)
                .collect(Collectors.toList());

        dto.setBuyItems(buyItems);
        dto.setSaleItems(saleItems);
        return dto;
    }

    private BuybackOrderItemDTO mapItemToDTO(BuybackOrderItem i) {
        return BuybackOrderItemDTO.builder()
                .id(i.getId())
                .itemType(i.getItemType().name())
                .commodityId(i.getCommodityId())
                .commodityName(i.getCommodityName())
                .unit(i.getUnit())
                .weight(i.getWeight())
                .conditionType(i.getConditionType() != null ? i.getConditionType().name() : null)
                .pricePerUnit(i.getPricePerUnit())
                .productName(i.getProductName())
                .quantity(i.getQuantity())
                .unitPrice(i.getUnitPrice())
                .totalPrice(i.getTotalPrice())
                .notes(i.getNotes())
                .build();
    }
}
