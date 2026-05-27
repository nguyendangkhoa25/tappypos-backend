package com.tappy.pos.repository.pawn;

import com.tappy.pos.model.entity.pawn.PawnWatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PawnWatchRepository extends JpaRepository<PawnWatchEntity, Long> {
    Optional<PawnWatchEntity> findByPawnId(Long pawnId);
    List<PawnWatchEntity> findByPawnIdIn(List<Long> pawnIds);

    @Modifying
    @Query("DELETE FROM PawnWatchEntity w WHERE w.pawnId = :pawnId")
    void deleteByPawnId(@Param("pawnId") Long pawnId);

    @Modifying
    @Query("DELETE FROM PawnWatchEntity w WHERE w.pawnId IN :pawnIds")
    void deleteByPawnIdIn(@Param("pawnIds") List<Long> pawnIds);
}
