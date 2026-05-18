package com.tappy.pos.repository.pawn;

import com.tappy.pos.model.entity.pawn.PawnAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PawnAuditRepository extends JpaRepository<PawnAuditEntity, Long> {
    List<PawnAuditEntity> findByPawnIdOrderByActionIdAsc(Long pawnId);
    void deleteAllByPawnIdIn(List<Long> pawnIds);
}
