package com.tappy.pos.repository.pawn;

import com.tappy.pos.model.entity.pawn.PawnQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PawnQueryRepository extends JpaRepository<PawnQuery, Long>, JpaSpecificationExecutor<PawnQuery>, PawnQueryRepositoryCustom {
}
