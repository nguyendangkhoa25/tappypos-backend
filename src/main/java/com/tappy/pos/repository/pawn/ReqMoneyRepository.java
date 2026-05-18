package com.tappy.pos.repository.pawn;

import com.tappy.pos.model.entity.pawn.ReqMoneyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReqMoneyRepository extends JpaRepository<ReqMoneyEntity, Long> {
    @Modifying
    @Query("delete from ReqMoneyEntity r where r.pawnId in (:pawnIds)")
    void deleteAllByPawnIdIn(List<Long> pawnIds);
}
