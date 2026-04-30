package com.knp.model.spec;

import com.knp.model.dto.pawn.SearchPawnRequest;
import com.knp.model.entity.pawn.PawnQuery;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

import static com.knp.model.spec.PawnSpecification.*;

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

        return spec;
    }

    public Specification<PawnQuery> buildPawnSpecForDeletion(SearchPawnRequest searchRequest) {
        Specification<PawnQuery> spec = buildPawnSpecifications(searchRequest);
        LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(3);
        spec = spec.and(excludeOldRedeemedItems(cutoffDate));
        return spec;
    }
}
