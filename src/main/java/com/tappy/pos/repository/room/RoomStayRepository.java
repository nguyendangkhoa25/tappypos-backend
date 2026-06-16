package com.tappy.pos.repository.room;

import com.tappy.pos.model.entity.room.RoomStayEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Tenant-scoped via PostgreSQL RLS (app.current_tenant). */
@Repository
public interface RoomStayRepository extends JpaRepository<RoomStayEntity, Long> {

    Optional<RoomStayEntity> findByIdAndDeletedFalse(Long id);

    /** The current in-house stay for a room, if any. */
    Optional<RoomStayEntity> findFirstByRoomIdAndStatusAndDeletedFalse(Long roomId, String status);

    /** All stays for a room in a given status (e.g. RESERVED) — used for overlap checks. */
    List<RoomStayEntity> findByRoomIdAndStatusAndDeletedFalse(Long roomId, String status);

    Page<RoomStayEntity> findByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    Page<RoomStayEntity> findByStatusAndDeletedFalseOrderByCreatedAtDesc(String status, Pageable pageable);

    long countByStatusAndDeletedFalse(String status);

    /** Reservations whose planned arrival falls within [from, to] — the calendar feed. */
    List<RoomStayEntity> findByStatusAndReservedCheckinBetweenAndDeletedFalseOrderByReservedCheckinAsc(
            String status, LocalDateTime from, LocalDateTime to);

    /** Upcoming reservations from a point onward (agenda list). */
    List<RoomStayEntity> findByStatusAndReservedCheckinGreaterThanEqualAndDeletedFalseOrderByReservedCheckinAsc(
            String status, LocalDateTime from);
}
