package com.tappy.pos.model.mapper;

import com.tappy.pos.model.dto.customer.CustomerDTO;
import com.tappy.pos.model.dto.pawn.*;
import com.tappy.pos.model.entity.pawn.*;
import com.tappy.pos.model.enums.PawnInterestCalculation;
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
        entity.setInterestCalcMode(req.getInterestCalcMode());
        entity.setHeldDays((int) req.getHeldDays());
        entity.setVisible(req.isVisible());
        entity.setPawnCategory(req.getPawnCategory());
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
        response.setInterestCalcMode(PawnInterestCalculation.fromCode(entity.getInterestCalcMode()).name());
        response.setVisible(entity.getVisible());
        response.setPawnCategory(entity.getPawnCategory());
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
        response.setInterestCalcMode(PawnInterestCalculation.fromCode(query.getInterestCalcMode()).name());
        response.setVisible(query.getVisible());
        response.setPawnCategory(query.getPawnCategory());
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
                .interestCalcMode(PawnInterestCalculation.fromCode(entity.getInterestCalcMode()).name())
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

    public PawnElectronicsDetail fromElectronicsEntity(PawnElectronicsEntity e) {
        if (e == null) return null;
        return PawnElectronicsDetail.builder()
                .brand(e.getBrand()).model(e.getModel()).imei(e.getImei())
                .storage(e.getStorage()).color(e.getColor()).condition(e.getCondition())
                .build();
    }

    public PawnElectronicsEntity toElectronicsEntity(String tenantId, Long pawnId, PawnElectronicsDetail d) {
        if (d == null) return null;
        return PawnElectronicsEntity.builder()
                .tenantId(tenantId).pawnId(pawnId)
                .brand(d.getBrand()).model(d.getModel()).imei(d.getImei())
                .storage(d.getStorage()).color(d.getColor()).condition(d.getCondition())
                .build();
    }

    public PawnVehicleDetail fromVehicleEntity(PawnVehicleEntity e) {
        if (e == null) return null;
        return PawnVehicleDetail.builder()
                .brand(e.getBrand()).model(e.getModel()).year(e.getYear())
                .licensePlate(e.getLicensePlate()).engineNumber(e.getEngineNumber())
                .chassisNumber(e.getChassisNumber()).color(e.getColor()).condition(e.getCondition())
                .build();
    }

    public PawnVehicleEntity toVehicleEntity(String tenantId, Long pawnId, PawnVehicleDetail d) {
        if (d == null) return null;
        return PawnVehicleEntity.builder()
                .tenantId(tenantId).pawnId(pawnId)
                .brand(d.getBrand()).model(d.getModel()).year(d.getYear())
                .licensePlate(d.getLicensePlate()).engineNumber(d.getEngineNumber())
                .chassisNumber(d.getChassisNumber()).color(d.getColor()).condition(d.getCondition())
                .build();
    }

    public PawnWatchDetail fromWatchEntity(PawnWatchEntity e) {
        if (e == null) return null;
        return PawnWatchDetail.builder()
                .brand(e.getBrand()).model(e.getModel())
                .material(e.getMaterial()).condition(e.getCondition())
                .build();
    }

    public PawnWatchEntity toWatchEntity(String tenantId, Long pawnId, PawnWatchDetail d) {
        if (d == null) return null;
        return PawnWatchEntity.builder()
                .tenantId(tenantId).pawnId(pawnId)
                .brand(d.getBrand()).model(d.getModel())
                .material(d.getMaterial()).condition(d.getCondition())
                .build();
    }

    public PawnRealEstateDetail fromRealEstateEntity(PawnRealEstateEntity e) {
        if (e == null) return null;
        return PawnRealEstateDetail.builder()
                .certificateNumber(e.getCertificateNumber()).certificateType(e.getCertificateType())
                .ownerName(e.getOwnerName()).address(e.getAddress())
                .areaSqm(e.getAreaSqm()).condition(e.getCondition())
                .build();
    }

    public PawnRealEstateEntity toRealEstateEntity(String tenantId, Long pawnId, PawnRealEstateDetail d) {
        if (d == null) return null;
        return PawnRealEstateEntity.builder()
                .tenantId(tenantId).pawnId(pawnId)
                .certificateNumber(d.getCertificateNumber()).certificateType(d.getCertificateType())
                .ownerName(d.getOwnerName()).address(d.getAddress())
                .areaSqm(d.getAreaSqm()).condition(d.getCondition())
                .build();
    }

    public PawnGeneralDetail fromGeneralEntity(PawnGeneralEntity e) {
        if (e == null) return null;
        return PawnGeneralDetail.builder()
                .serialNumber(e.getSerialNumber()).condition(e.getCondition())
                .build();
    }

    public PawnGeneralEntity toGeneralEntity(String tenantId, Long pawnId, PawnGeneralDetail d) {
        if (d == null) return null;
        return PawnGeneralEntity.builder()
                .tenantId(tenantId).pawnId(pawnId)
                .serialNumber(d.getSerialNumber()).condition(d.getCondition())
                .build();
    }
}
