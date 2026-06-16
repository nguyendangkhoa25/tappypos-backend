package com.tappy.pos.repository.pawn;

import com.tappy.pos.model.entity.pawn.PawnRealEstateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PawnRealEstateRepository extends JpaRepository<PawnRealEstateEntity, Long> {
    Optional<PawnRealEstateEntity> findByPawnId(Long pawnId);
    List<PawnRealEstateEntity> findByPawnIdIn(List<Long> pawnIds);

    @Modifying
    @Query("DELETE FROM PawnRealEstateEntity r WHERE r.pawnId = :pawnId")
    void deleteByPawnId(@Param("pawnId") Long pawnId);

    @Modifying
    @Query("DELETE FROM PawnRealEstateEntity r WHERE r.pawnId IN :pawnIds")
    void deleteByPawnIdIn(@Param("pawnIds") List<Long> pawnIds);
}
