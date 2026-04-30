package com.knp.model.mapper;

import com.knp.model.dto.customer.CustomerDTO;
import com.knp.model.dto.pawn.*;
import com.knp.model.entity.pawn.PawnAuditEntity;
import com.knp.model.entity.pawn.PawnEntity;
import com.knp.model.entity.pawn.PawnQuery;
import com.knp.model.entity.pawn.ReqMoneyEntity;
import org.springframework.stereotype.Component;

@Component
public class PawnMapper {

    public PawnEntity fromPawnRequest(PawnRequest req) {
        if (req == null) return null;
        PawnEntity entity = new PawnEntity();
        entity.setCustomerId(req.getCustomerId());
        entity.setItemName(req.getItemName());
        entity.setItemDescription(req.getItemDescription());
        entity.setItemWeight(req.getItemWeight());
        entity.setGemWeight(req.getGemWeight());
        entity.setItemValue(req.getItemValue());
        entity.setItemType(req.getItemType());
        entity.setItemBrand(req.getItemBrand());
        entity.setPawnDate(req.getPawnDate());
        entity.setPawnDueDate(req.getPawnDueDate());
        entity.setPawnAmount(req.getPawnAmount());
        entity.setInterestRate(req.getInterestRate());
        entity.setTotalAmount(req.getTotalAmount());
        entity.setPawnStatus(req.getPawnStatus());
        entity.setRedeemDate(req.getRedeemDate());
        entity.setInterestAmount(req.getInterestAmount());
        entity.setForfeitedDate(req.getForfeitedDate());
        entity.setForfeitedReason(req.getForfeitedReason());
        entity.setForfeitedAmount(req.getForfeitedAmount());
        entity.setInterestDaysPerMonth(req.getInterestDaysPerMonth());
        entity.setHeldDays((int) req.getHeldDays());
        entity.setVisible(req.isVisible());
        return entity;
    }

    public PawnResponse fromPawnEntity(PawnEntity entity) {
        if (entity == null) return null;
        PawnResponse response = new PawnResponse();
        response.setPawnId(entity.getPawnId());
        response.setCustomerId(entity.getCustomerId());
        response.setItemName(entity.getItemName());
        response.setItemBrand(entity.getItemBrand());
        response.setItemType(entity.getItemType());
        response.setItemDescription(entity.getItemDescription());
        response.setItemValue(entity.getItemValue());
        response.setItemWeight(entity.getItemWeight());
        response.setGemWeight(entity.getGemWeight());
        response.setPawnDate(entity.getPawnDate());
        response.setPawnDueDate(entity.getPawnDueDate());
        response.setPawnAmount(entity.getPawnAmount());
        response.setInterestRate(entity.getInterestRate());
        response.setRedeemDate(entity.getRedeemDate());
        response.setInterestAmount(entity.getInterestAmount());
        response.setTotalAmount(entity.getTotalAmount());
        response.setHeldDays(entity.getHeldDays() != null ? entity.getHeldDays() : 0L);
        response.setPawnStatus(entity.getPawnStatus());
        response.setCreatedBy(entity.getCreatedBy());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedBy(entity.getUpdatedBy());
        response.setUpdatedAt(entity.getUpdatedAt());
        response.setCanceledReason(entity.getCanceledReason());
        response.setForfeitedReason(entity.getForfeitedReason());
        response.setForfeitedDate(entity.getForfeitedDate());
        response.setForfeitedAmount(entity.getForfeitedAmount());
        response.setInterestDaysPerMonth(entity.getInterestDaysPerMonth());
        response.setVisible(entity.getVisible());
        // customerName and phone set separately from CustomerService
        return response;
    }

    public PawnResponse fromPawnQuery(PawnQuery query) {
        if (query == null) return null;
        PawnResponse response = new PawnResponse();
        response.setPawnId(query.getPawnId());
        response.setItemName(query.getItemName());
        response.setItemBrand(query.getItemBrand());
        response.setItemType(query.getItemType());
        response.setItemDescription(query.getItemDescription());
        response.setItemValue(query.getItemValue());
        response.setItemWeight(query.getItemWeight());
        response.setGemWeight(query.getGemWeight());
        response.setPawnDate(query.getPawnDate());
        response.setPawnDueDate(query.getPawnDueDate());
        response.setPawnAmount(query.getPawnAmount());
        response.setInterestRate(query.getInterestRate());
        response.setRedeemDate(query.getRedeemDate());
        response.setInterestAmount(query.getInterestAmount());
        response.setTotalAmount(query.getTotalAmount());
        response.setPawnStatus(query.getPawnStatus());
        response.setCreatedBy(query.getCreatedBy());
        response.setCreatedAt(query.getCreatedAt());
        response.setUpdatedBy(query.getUpdatedBy());
        response.setUpdatedAt(query.getUpdatedAt());
        response.setCanceledReason(query.getCanceledReason());
        response.setForfeitedReason(query.getForfeitedReason());
        response.setForfeitedDate(query.getForfeitedDate());
        response.setForfeitedAmount(query.getForfeitedAmount());
        response.setInterestDaysPerMonth(query.getInterestDaysPerMonth());
        response.setVisible(query.getVisible());
        if (query.getCustomer() != null) {
            response.setCustomerId(query.getCustomer().getId());
            response.setCustomerName(query.getCustomer().getName());
            response.setPhone(query.getCustomer().getPhone());
            CustomerDTO customerDTO = CustomerDTO.builder()
                    .id(query.getCustomer().getId())
                    .name(query.getCustomer().getName())
                    .phone(query.getCustomer().getPhone())
                    .build();
            response.setCustomer(customerDTO);
        }
        return response;
    }

    public PawnAudit auditFromPawnAuditEntity(PawnAuditEntity entity) {
        if (entity == null) return null;
        return PawnAudit.builder()
                .actionId(entity.getActionId())
                .actionType(entity.getActionType())
                .actionTime(entity.getActionTime())
                .customerId(entity.getCustomerId())
                .itemName(entity.getItemName())
                .itemDescription(entity.getItemDescription())
                .itemWeight(entity.getItemWeight())
                .gemWeight(entity.getGemWeight())
                .itemValue(entity.getItemValue())
                .itemType(entity.getItemType())
                .itemBrand(entity.getItemBrand())
                .pawnDate(entity.getPawnDate())
                .pawnDueDate(entity.getPawnDueDate())
                .pawnAmount(entity.getPawnAmount())
                .interestRate(entity.getInterestRate())
                .pawnStatus(entity.getPawnStatus())
                .canceledReason(entity.getCanceledReason())
                .totalAmount(entity.getTotalAmount())
                .redeemDate(entity.getRedeemDate())
                .interestAmount(entity.getInterestAmount())
                .forfeitedReason(entity.getForfeitedReason())
                .forfeitedAmount(entity.getForfeitedAmount())
                .forfeitedDate(entity.getForfeitedDate())
                .interestDaysPerMonth(entity.getInterestDaysPerMonth())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedBy(entity.getUpdatedBy())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public ReqMoneyResponse fromReqMoneyEntity(ReqMoneyEntity entity) {
        if (entity == null) return null;
        ReqMoneyResponse response = new ReqMoneyResponse();
        response.setRequestId(entity.getRequestId());
        response.setPawnId(entity.getPawnId());
        response.setRequestDate(entity.getRequestDate());
        response.setRequestAmount(entity.getRequestAmount());
        return response;
    }
}
