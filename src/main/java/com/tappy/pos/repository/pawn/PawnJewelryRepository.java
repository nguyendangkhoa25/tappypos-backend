package com.tappy.pos.repository.pawn;

import com.tappy.pos.model.entity.pawn.PawnJewelryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PawnJewelryRepository extends JpaRepository<PawnJewelryEntity, Long> {
    Optional<PawnJewelryEntity> findByPawnId(Long pawnId);
    List<PawnJewelryEntity> findByPawnIdIn(List<Long> pawnIds);

    @Modifying
    @Query("DELETE FROM PawnJewelryEntity j WHERE j.pawnId = :pawnId")
    void deleteByPawnId(@Param("pawnId") Long pawnId);

    @Modifying
    @Query("DELETE FROM PawnJewelryEntity j WHERE j.pawnId IN :pawnIds")
    void deleteByPawnIdIn(@Param("pawnIds") List<Long> pawnIds);
}
