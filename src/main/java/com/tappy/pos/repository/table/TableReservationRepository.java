package com.tappy.pos.repository.table;

import com.tappy.pos.model.entity.table.TableReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TableReservationRepository extends JpaRepository<TableReservation, Long> {

    /** Reservations within a datetime window (RLS scopes to the current tenant), soonest first. */
    @Query("""
        SELECT r FROM TableReservation r
        WHERE r.deleted = false AND r.reservedAt >= :from AND r.reservedAt < :to
        ORDER BY r.reservedAt ASC
    """)
    List<TableReservation> findInRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
