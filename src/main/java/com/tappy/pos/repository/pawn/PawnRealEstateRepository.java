package com.tappy.pos.repository.pawn;

import com.tappy.pos.model.entity.pawn.PawnRealEstateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PawnRealEstateRepository extends JpaRepository<PawnRealEstateEntity, Long> {
    Optional<PawnRealEstateEntity> findByPawnId(Long pawnId);
    List<PawnRealEstateEntity> findByPawnIdIn(List<Long> pawnIds);
    void deleteByPawnId(Long pawnId);
    void deleteByPawnIdIn(List<Long> pawnIds);
}
