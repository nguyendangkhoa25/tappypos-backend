package com.tappy.pos.repository.pawn;

import com.tappy.pos.model.entity.pawn.PawnVehicleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PawnVehicleRepository extends JpaRepository<PawnVehicleEntity, Long> {
    Optional<PawnVehicleEntity> findByPawnId(Long pawnId);
    List<PawnVehicleEntity> findByPawnIdIn(List<Long> pawnIds);

    @Modifying
    @Query("DELETE FROM PawnVehicleEntity v WHERE v.pawnId = :pawnId")
    void deleteByPawnId(@Param("pawnId") Long pawnId);

    @Modifying
    @Query("DELETE FROM PawnVehicleEntity v WHERE v.pawnId IN :pawnIds")
    void deleteByPawnIdIn(@Param("pawnIds") List<Long> pawnIds);
}
