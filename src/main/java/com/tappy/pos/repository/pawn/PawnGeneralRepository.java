package com.tappy.pos.repository.pawn;

import com.tappy.pos.model.entity.pawn.PawnGeneralEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PawnGeneralRepository extends JpaRepository<PawnGeneralEntity, Long> {
    Optional<PawnGeneralEntity> findByPawnId(Long pawnId);
    List<PawnGeneralEntity> findByPawnIdIn(List<Long> pawnIds);

    @Modifying
    @Query("DELETE FROM PawnGeneralEntity g WHERE g.pawnId = :pawnId")
    void deleteByPawnId(@Param("pawnId") Long pawnId);

    @Modifying
    @Query("DELETE FROM PawnGeneralEntity g WHERE g.pawnId IN :pawnIds")
    void deleteByPawnIdIn(@Param("pawnIds") List<Long> pawnIds);
}
