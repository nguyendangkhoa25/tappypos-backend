package com.tappy.pos.repository.pawn;

import com.tappy.pos.model.entity.pawn.PawnElectronicsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PawnElectronicsRepository extends JpaRepository<PawnElectronicsEntity, Long> {
    Optional<PawnElectronicsEntity> findByPawnId(Long pawnId);
    List<PawnElectronicsEntity> findByPawnIdIn(List<Long> pawnIds);

    @Modifying
    @Query("DELETE FROM PawnElectronicsEntity e WHERE e.pawnId = :pawnId")
    void deleteByPawnId(@Param("pawnId") Long pawnId);

    @Modifying
    @Query("DELETE FROM PawnElectronicsEntity e WHERE e.pawnId IN :pawnIds")
    void deleteByPawnIdIn(@Param("pawnIds") List<Long> pawnIds);
}
