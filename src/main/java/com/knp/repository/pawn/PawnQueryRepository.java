package com.knp.repository.pawn;

import com.knp.model.entity.pawn.PawnQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

@Repository
public interface PawnQueryRepository extends JpaRepository<PawnQuery, Long>, JpaSpecificationExecutor<PawnQuery> {
    @EntityGraph(attributePaths = {"reqMoneys", "customer"})
    @NonNull
    Page<PawnQuery> findAll(Specification<PawnQuery> spec, @NonNull Pageable pageable);
}
