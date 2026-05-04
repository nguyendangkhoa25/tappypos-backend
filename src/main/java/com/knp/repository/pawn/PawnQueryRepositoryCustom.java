package com.knp.repository.pawn;

import com.knp.model.dto.pawn.PawnSummary;
import com.knp.model.entity.pawn.PawnQuery;
import org.springframework.data.jpa.domain.Specification;

public interface PawnQueryRepositoryCustom {
    PawnSummary getSummary(Specification<PawnQuery> spec);
}
