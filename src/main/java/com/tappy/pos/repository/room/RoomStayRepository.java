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

    /** Checked-out stays whose checkout falls in [from, to) — for lodging analytics. */
    @org.springframework.data.jpa.repository.Query(
        "SELECT s FROM RoomStayEntity s WHERE s.deleted = false AND s.status = 'CHECKED_OUT' " +
        "AND s.checkoutAt >= :from AND s.checkoutAt < :to")
    List<RoomStayEntity> findCheckedOutBetween(@org.springframework.data.repository.query.Param("from") java.time.LocalDateTime from,
                                               @org.springframework.data.repository.query.Param("to") java.time.LocalDateTime to);

    /** The current in-house stay for a room, if any. */
    Optional<RoomStayEntity> findFirstByRoomIdAndStatusAndDeletedFalse(Long roomId, String status);

    /** All stays for a room in a given status (e.g. RESERVED) — used for overlap checks. */
    List<RoomStayEntity> findByRoomIdAndStatusAndDeletedFalse(Long roomId, String status);

    /** All stays for a room in any of the given statuses (RESERVED + IN_HOUSE) — overlap checks. */
    List<RoomStayEntity> findByRoomIdAndStatusInAndDeletedFalse(Long roomId, java.util.Collection<String> statuses);

    /** Active reservations due on/before a cutoff (the board surfaces these for front-desk check-in). */
    List<RoomStayEntity> findByStatusAndReservedCheckinLessThanEqualAndDeletedFalseOrderByReservedCheckinAsc(
            String status, LocalDateTime cutoff);

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
