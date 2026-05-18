package com.tappy.pos.repository.pawn;

import com.tappy.pos.model.entity.pawn.PawnElectronicsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PawnElectronicsRepository extends JpaRepository<PawnElectronicsEntity, Long> {
    Optional<PawnElectronicsEntity> findByPawnId(Long pawnId);
    List<PawnElectronicsEntity> findByPawnIdIn(List<Long> pawnIds);
    void deleteByPawnId(Long pawnId);
    void deleteByPawnIdIn(List<Long> pawnIds);
}
