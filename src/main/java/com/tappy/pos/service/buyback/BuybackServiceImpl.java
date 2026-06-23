package com.tappy.pos.service.buyback;

import com.tappy.pos.config.AuthContext;
import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.buyback.BuybackResponse;
import com.tappy.pos.model.dto.buyback.CreateBuybackRequest;
import com.tappy.pos.model.dto.buyback.SellBuybackRequest;
import com.tappy.pos.model.entity.buyback.BuybackEntity;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.model.enums.BuybackStatus;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.buyback.BuybackRepository;
import com.tappy.pos.repository.customer.CustomerRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BuybackServiceImpl implements BuybackService {

    private final BuybackRepository buybackRepository;
    private final CustomerRepository customerRepository;
    private final AuthContext authContext;
    private final TenantContext tenantContext;
    private final ActivityLogService activityLogService;
    private final MessageService messageService;

    @Override
    public BuybackResponse createBuyback(CreateBuybackRequest request) {
        String actor = authContext.getCurrentUsername();
        LocalDateTime now = LocalDateTime.now();

        String customerName = request.getCustomerName();
        if (StringUtils.isBlank(customerName) && request.getCustomerId() != null) {
            customerName = customerRepository.findById(request.getCustomerId())
                    .map(c -> c.getName()).orElse(null);
        }

        BuybackEntity entity = BuybackEntity.builder()
                .tenantId(tenantContext.getCurrentTenantId())
                .customerId(request.getCustomerId())
                .customerName(customerName)
                .itemName(request.getItemName())
                .itemDescription(request.getItemDescription())
                .itemCategory(request.getItemCategory())
                .acquisitionPrice(request.getAcquisitionPrice())
                .status(BuybackStatus.PURCHASED)
                .purchaseDate(request.getPurchaseDate() != null ? request.getPurchaseDate() : now)
                .visible(true)
                .createdBy(actor)
                .updatedBy(actor)
                .createdAt(now)
                .updatedAt(now)
                .build();

        BuybackEntity saved = buybackRepository.save(entity);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), actor, null,
                ActivityAction.BUYBACK_CREATED, "BUYBACK", String.valueOf(saved.getBuybackId()),
                "activity.buyback.created", null, saved.getItemName());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BuybackResponse getBuyback(Long buybackId) {
        return toResponse(load(buybackId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BuybackResponse> getBuybacks(BuybackStatus status, Pageable pageable) {
        return buybackRepository.findAllVisible(status, pageable).map(this::toResponse);
    }

    @Override
    public BuybackResponse markSold(Long buybackId, SellBuybackRequest request) {
        BuybackEntity entity = load(buybackId);
        if (entity.getStatus() == BuybackStatus.SOLD || entity.getStatus() == BuybackStatus.CANCELLED) {
            throw new BadRequestException(messageService.getMessage(
                    "error.buyback.statusNotAllowed", new Object[]{entity.getStatus()}));
        }
        entity.setResalePrice(request.getResalePrice());
        entity.setOrderId(request.getOrderId());
        entity.setStatus(BuybackStatus.SOLD);
        entity.setSoldDate(LocalDateTime.now());
        entity.setUpdatedBy(authContext.getCurrentUsername());
        entity.setUpdatedAt(LocalDateTime.now());
        buybackRepository.save(entity);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.BUYBACK_SOLD, "BUYBACK", String.valueOf(buybackId),
                "activity.buyback.sold", null, entity.getItemName());
        return toResponse(entity);
    }

    @Override
    public BuybackResponse cancelBuyback(Long buybackId, String reason) {
        BuybackEntity entity = load(buybackId);
        if (entity.getStatus() == BuybackStatus.SOLD) {
            throw new BadRequestException(messageService.getMessage(
                    "error.buyback.statusNotAllowed", new Object[]{entity.getStatus()}));
        }
        entity.setStatus(BuybackStatus.CANCELLED);
        entity.setCanceledReason(reason);
        entity.setUpdatedBy(authContext.getCurrentUsername());
        entity.setUpdatedAt(LocalDateTime.now());
        buybackRepository.save(entity);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.BUYBACK_CANCELLED, "BUYBACK", String.valueOf(buybackId),
                "activity.buyback.cancelled", null, entity.getItemName());
        return toResponse(entity);
    }

    private BuybackEntity load(Long buybackId) {
        return buybackRepository.findById(buybackId).orElseThrow(ResourceNotFoundException::new);
    }

    private BuybackResponse toResponse(BuybackEntity e) {
        BigDecimal margin = (e.getStatus() == BuybackStatus.SOLD && e.getResalePrice() != null)
                ? e.getResalePrice().subtract(e.getAcquisitionPrice()) : null;
        String idNumber = e.getCustomerId() == null ? null
                : customerRepository.findById(e.getCustomerId()).map(c -> c.getIdNumber()).orElse(null);
        return BuybackResponse.builder()
                .buybackId(e.getBuybackId())
                .customerId(e.getCustomerId())
                .customerName(e.getCustomerName())
                .customerIdNumber(idNumber)
                .itemName(e.getItemName())
                .itemDescription(e.getItemDescription())
                .itemCategory(e.getItemCategory())
                .acquisitionPrice(e.getAcquisitionPrice())
                .resalePrice(e.getResalePrice())
                .margin(margin)
                .status(e.getStatus())
                .productId(e.getProductId())
                .orderId(e.getOrderId())
                .purchaseDate(e.getPurchaseDate())
                .soldDate(e.getSoldDate())
                .canceledReason(e.getCanceledReason())
                .createdBy(e.getCreatedBy())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
