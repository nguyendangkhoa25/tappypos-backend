package com.tappy.pos.repository.booking;

import com.tappy.pos.model.entity.booking.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByIdAndTenantIdAndDeletedFalse(Long id, String tenantId);

    /** Bookings on a given day (by scheduled date for reservations, or started date for walk-ins). */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.tenantId = :tenantId
          AND b.deleted = false
          AND (b.scheduledDate = :date OR CAST(b.startedAt AS LocalDate) = :date)
          AND (:status IS NULL OR b.status = :status)
        ORDER BY b.scheduledStartTime ASC NULLS LAST, b.startedAt ASC NULLS LAST, b.id DESC
        """)
    Page<Booking> findByDate(
            @Param("tenantId") String tenantId,
            @Param("date") LocalDate date,
            @Param("status") String status,
            Pageable pageable);

    /** All currently-running sessions for the tenant (the live "occupied" view). */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.tenantId = :tenantId
          AND b.deleted = false
          AND b.status = 'IN_PROGRESS'
        ORDER BY b.startedAt ASC
        """)
    List<Booking> findInProgress(@Param("tenantId") String tenantId);

    /** The single running session on a resource, if any. */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.tenantId = :tenantId
          AND b.deleted = false
          AND b.resourceId = :resourceId
          AND b.status = 'IN_PROGRESS'
        """)
    Optional<Booking> findActiveByResource(
            @Param("tenantId") String tenantId,
            @Param("resourceId") Long resourceId);

    /** Active reservations on a resource for a date — used for overlap detection (time check in Java). */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.tenantId = :tenantId
          AND b.deleted = false
          AND b.resourceId = :resourceId
          AND b.bookingType = 'RESERVATION'
          AND b.scheduledDate = :date
          AND b.status NOT IN ('CANCELLED', 'NO_SHOW', 'COMPLETED')
          AND b.id <> :excludeId
        """)
    List<Booking> findReservationsByResourceAndDate(
            @Param("tenantId") String tenantId,
            @Param("resourceId") Long resourceId,
            @Param("date") LocalDate date,
            @Param("excludeId") Long excludeId);

    @Query("""
        SELECT COUNT(b) + 1 FROM Booking b
        WHERE b.tenantId = :tenantId
          AND CAST(b.createdAt AS LocalDate) = :today
        """)
    long countTodayByTenantId(@Param("tenantId") String tenantId, @Param("today") LocalDate today);
}
