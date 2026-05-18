package com.tappy.pos.model.spec;

import com.tappy.pos.model.dto.pawn.DateFilterRequest;
import com.tappy.pos.model.dto.pawn.SearchPawnRequest;
import com.tappy.pos.model.entity.pawn.PawnQuery;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static com.tappy.pos.model.spec.PawnSpecification.*;

public class PawnSpecificationBuilder {

    public Specification<PawnQuery> buildPawnSpecifications(SearchPawnRequest searchRequest) {
        Specification<PawnQuery> spec = Specification.where(null);
        if (searchRequest == null) return spec;

        if (searchRequest.getPawnId() != null) {
            spec = spec.and(searchPawnId(searchRequest.getPawnId()));
            return spec;
        }

        if (searchRequest.getPawnStatuses() != null && !searchRequest.getPawnStatuses().isEmpty()) {
            spec = spec.and(searchStatus(searchRequest.getPawnStatuses()));
        }

        if (StringUtils.isNotEmpty(searchRequest.getSearchWord())) {
            spec = spec.and(freeSearch(searchRequest.getSearchWord())
                    .or(searchCustomer(searchRequest.getSearchWord())));
        }

        if (searchRequest.getCustomerId() != null) {
            spec = spec.and(searchCustomerId(searchRequest.getCustomerId()));
        }

        if (searchRequest.getPawnAmount() != null) {
            spec = spec.and(searchAmount(searchRequest.getPawnAmount()));
        }

        if (StringUtils.isNotEmpty(searchRequest.getItemName())) {
            spec = spec.and(searchByItemName(searchRequest.getItemName()));
        }

        if (searchRequest.getPawnDueDate() != null) {
            spec = spec.and(searchPawnDueDate(searchRequest.getPawnDueDate()));
        }

        if (searchRequest.getPawnDate() != null) {
            spec = spec.and(searchPawnDate(searchRequest.getPawnDate()));
        }

        if (searchRequest.getRedeemDate() != null) {
            spec = spec.and(searchRedeemDate(searchRequest.getRedeemDate()));
        }

        if (searchRequest.getForfeitedDate() != null) {
            spec = spec.and(searchForfeitedDate(searchRequest.getForfeitedDate()));
        }

        if (searchRequest.getRequestDate() != null) {
            spec = spec.and(searchRequestMoneyRequestDate(searchRequest.getRequestDate()));
        }

        if (StringUtils.isNotEmpty(searchRequest.getPawnCategory())) {
            spec = spec.and(filterByPawnCategory(searchRequest.getPawnCategory()));
            spec = applyAttributeFilters(spec, searchRequest);
        }

        if (Boolean.TRUE.equals(searchRequest.getTodayFilter())) {
            ZoneId zone = ZoneId.systemDefault();
            LocalDateTime startOfDay = LocalDate.now(zone).atStartOfDay();
            LocalDateTime endOfDay   = startOfDay.plusDays(1).minusNanos(1);
            long from = startOfDay.atZone(zone).toInstant().toEpochMilli();
            long to   = endOfDay.atZone(zone).toInstant().toEpochMilli();
            DateFilterRequest todayRange = DateFilterRequest.builder().fromDate(from).toDate(to).build();
            spec = spec.and(
                searchPawnDate(todayRange)
                    .or(searchRedeemDate(todayRange))
                    .or(searchForfeitedDate(todayRange))
            );
        }

        return spec;
    }

    private Specification<PawnQuery> applyAttributeFilters(Specification<PawnQuery> spec, SearchPawnRequest req) {
        String category = req.getPawnCategory();
        switch (category) {
            case "ELECTRONICS" -> {
                if (StringUtils.isNotEmpty(req.getBrand()))
                    spec = spec.and(filterByElectronicsAttribute("brand", req.getBrand()));
                if (StringUtils.isNotEmpty(req.getModel()))
                    spec = spec.and(filterByElectronicsAttribute("model", req.getModel()));
                if (StringUtils.isNotEmpty(req.getImei()))
                    spec = spec.and(filterByElectronicsAttribute("imei", req.getImei()));
                if (StringUtils.isNotEmpty(req.getCondition()))
                    spec = spec.and(filterByElectronicsAttribute("condition", req.getCondition()));
            }
            case "MOTORBIKE", "CAR" -> {
                if (StringUtils.isNotEmpty(req.getBrand()))
                    spec = spec.and(filterByVehicleAttribute("brand", req.getBrand()));
                if (StringUtils.isNotEmpty(req.getModel()))
                    spec = spec.and(filterByVehicleAttribute("model", req.getModel()));
                if (StringUtils.isNotEmpty(req.getLicensePlate()))
                    spec = spec.and(filterByVehicleAttribute("licensePlate", req.getLicensePlate()));
                if (StringUtils.isNotEmpty(req.getCondition()))
                    spec = spec.and(filterByVehicleAttribute("condition", req.getCondition()));
            }
            case "WATCH" -> {
                if (StringUtils.isNotEmpty(req.getBrand()))
                    spec = spec.and(filterByWatchAttribute("brand", req.getBrand()));
                if (StringUtils.isNotEmpty(req.getModel()))
                    spec = spec.and(filterByWatchAttribute("model", req.getModel()));
                if (StringUtils.isNotEmpty(req.getCondition()))
                    spec = spec.and(filterByWatchAttribute("condition", req.getCondition()));
            }
            case "REAL_ESTATE" -> {
                if (StringUtils.isNotEmpty(req.getBrand()))
                    spec = spec.and(filterByRealEstateAttribute("certificateNumber", req.getBrand()));
                if (StringUtils.isNotEmpty(req.getModel()))
                    spec = spec.and(filterByRealEstateAttribute("ownerName", req.getModel()));
                if (StringUtils.isNotEmpty(req.getCondition()))
                    spec = spec.and(filterByRealEstateAttribute("condition", req.getCondition()));
            }
            case "GENERAL" -> {
                if (StringUtils.isNotEmpty(req.getImei()))
                    spec = spec.and(filterByGeneralAttribute("serialNumber", req.getImei()));
                if (StringUtils.isNotEmpty(req.getCondition()))
                    spec = spec.and(filterByGeneralAttribute("condition", req.getCondition()));
            }
        }
        return spec;
    }

    public Specification<PawnQuery> buildPawnSpecForDeletion(SearchPawnRequest searchRequest) {
        Specification<PawnQuery> spec = buildPawnSpecifications(searchRequest);
        LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(3);
        spec = spec.and(excludeOldRedeemedItems(cutoffDate));
        return spec;
    }
}
