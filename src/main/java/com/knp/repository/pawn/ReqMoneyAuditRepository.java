package com.knp.repository.pawn;

import com.knp.model.entity.pawn.ReqMoneyAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReqMoneyAuditRepository extends JpaRepository<ReqMoneyAuditEntity, Long> {
    void deleteAllByPawnIdIn(List<Long> pawnIds);
}
