package com.tappy.pos.repository.pawn;

import com.tappy.pos.model.dto.pawn.PawnSummary;
import com.tappy.pos.model.entity.pawn.PawnQuery;
import com.tappy.pos.model.entity.pawn.ReqMoneyEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class PawnQueryRepositoryImpl implements PawnQueryRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public PawnSummary getSummary(Specification<PawnQuery> spec) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        // Step 1: collect matching pawn IDs (DISTINCT handles any joins inside the spec)
        CriteriaQuery<Long> idsQuery = cb.createQuery(Long.class);
        Root<PawnQuery> idsRoot = idsQuery.from(PawnQuery.class);
        idsQuery.select(idsRoot.get("pawnId")).distinct(true);
        Predicate idsPred = spec.toPredicate(idsRoot, idsQuery, cb);
        if (idsPred != null) idsQuery.where(idsPred);
        List<Long> pawnIds = em.createQuery(idsQuery).getResultList();

        if (pawnIds.isEmpty()) {
            return PawnSummary.builder()
                    .totalCount(0)
                    .totalWeight(BigDecimal.ZERO)
                    .totalAmount(0L)
                    .build();
        }

        // Step 2: aggregate gold_weight (from jewelry detail) and pawn_amount.
        // Weight lives in pawn_item_jewelry since V007; non-jewelry rows contribute 0.
        Object[] result = (Object[]) em.createNativeQuery(
                "SELECT COUNT(p.pawn_id), " +
                "       COALESCE(SUM(pij.gold_weight), 0), " +
                "       COALESCE(SUM(p.pawn_amount), 0) " +
                "FROM pawn p " +
                "LEFT JOIN pawn_item_jewelry pij ON pij.pawn_id = p.pawn_id " +
                "WHERE p.pawn_id IN (:ids)")
                .setParameter("ids", pawnIds)
                .getSingleResult();

        long totalCount        = ((Number) result[0]).longValue();
        BigDecimal totalWeight     = (BigDecimal) result[1];
        BigDecimal totalPawnAmount = (BigDecimal) result[2];

        // Step 3: sum additional money requests for the same pawn IDs
        CriteriaQuery<BigDecimal> reqQuery = cb.createQuery(BigDecimal.class);
        Root<ReqMoneyEntity> reqRoot = reqQuery.from(ReqMoneyEntity.class);
        reqQuery.where(reqRoot.get("pawnId").in(pawnIds));
        reqQuery.select(cb.coalesce(cb.sum(reqRoot.get("requestAmount")), BigDecimal.ZERO));
        BigDecimal totalReqAmount = em.createQuery(reqQuery).getSingleResult();

        return PawnSummary.builder()
                .totalCount(totalCount)
                .totalWeight(totalWeight)
                .totalAmount(totalPawnAmount.add(totalReqAmount).longValue())
                .build();
    }
}
