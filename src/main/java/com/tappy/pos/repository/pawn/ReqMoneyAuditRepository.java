package com.tappy.pos.repository.pawn;

import com.tappy.pos.model.entity.pawn.ReqMoneyAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReqMoneyAuditRepository extends JpaRepository<ReqMoneyAuditEntity, Long> {
    void deleteAllByPawnIdIn(List<Long> pawnIds);
}
