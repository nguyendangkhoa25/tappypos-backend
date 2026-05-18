package com.tappy.pos.repository.pawn;

import com.tappy.pos.model.entity.pawn.PawnVehicleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PawnVehicleRepository extends JpaRepository<PawnVehicleEntity, Long> {
    Optional<PawnVehicleEntity> findByPawnId(Long pawnId);
    List<PawnVehicleEntity> findByPawnIdIn(List<Long> pawnIds);
    void deleteByPawnId(Long pawnId);
    void deleteByPawnIdIn(List<Long> pawnIds);
}
