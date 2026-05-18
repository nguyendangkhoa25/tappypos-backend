package com.tappy.pos.repository.pawn;

import com.tappy.pos.model.dto.pawn.PawnSummary;
import com.tappy.pos.model.entity.pawn.PawnQuery;
import org.springframework.data.jpa.domain.Specification;

public interface PawnQueryRepositoryCustom {
    PawnSummary getSummary(Specification<PawnQuery> spec);
}
