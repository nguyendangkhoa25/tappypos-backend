package com.tappy.pos.repository.pawn;

import com.tappy.pos.model.entity.pawn.PawnWatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PawnWatchRepository extends JpaRepository<PawnWatchEntity, Long> {
    Optional<PawnWatchEntity> findByPawnId(Long pawnId);
    List<PawnWatchEntity> findByPawnIdIn(List<Long> pawnIds);
    void deleteByPawnId(Long pawnId);
    void deleteByPawnIdIn(List<Long> pawnIds);
}
