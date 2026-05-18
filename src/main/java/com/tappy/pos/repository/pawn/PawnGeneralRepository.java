package com.tappy.pos.repository.pawn;

import com.tappy.pos.model.entity.pawn.PawnGeneralEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PawnGeneralRepository extends JpaRepository<PawnGeneralEntity, Long> {
    Optional<PawnGeneralEntity> findByPawnId(Long pawnId);
    List<PawnGeneralEntity> findByPawnIdIn(List<Long> pawnIds);
    void deleteByPawnId(Long pawnId);
    void deleteByPawnIdIn(List<Long> pawnIds);
}
