package com.tappy.pos.repository.appointment;

import com.tappy.pos.model.entity.appointment.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("""
        SELECT a FROM Appointment a
        WHERE a.tenantId = :tenantId
          AND a.scheduledDate = :date
          AND a.deleted = false
        ORDER BY a.scheduledStartTime ASC
        """)
    Page<Appointment> findByTenantIdAndDate(
            @Param("tenantId") String tenantId,
            @Param("date") LocalDate date,
            Pageable pageable);

    @Query("""
        SELECT a FROM Appointment a
        WHERE a.tenantId = :tenantId
          AND a.scheduledDate >= :fromDate
          AND a.scheduledDate <= :toDate
          AND a.deleted = false
        ORDER BY a.scheduledDate ASC, a.scheduledStartTime ASC
        """)
    Page<Appointment> findByTenantIdAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable);

    Optional<Appointment> findByIdAndTenantIdAndDeletedFalse(Long id, String tenantId);

    @Query("""
        SELECT COUNT(a) + 1 FROM Appointment a
        WHERE a.tenantId = :tenantId
          AND CAST(a.createdAt AS LocalDate) = :today
        """)
    long countTodayByTenantId(@Param("tenantId") String tenantId, @Param("today") LocalDate today);

    // ── Week summary ──────────────────────────────────────────────────────────

    /**
     * Returns (scheduledDate, count) pairs for the given date range.
     * Days with zero appointments are absent from the result.
     */
    @Query("""
        SELECT a.scheduledDate, COUNT(a)
        FROM Appointment a
        WHERE a.tenantId = :tenantId
          AND a.scheduledDate BETWEEN :from AND :to
          AND a.deleted = false
        GROUP BY a.scheduledDate
        """)
    List<Object[]> countByDateRange(
            @Param("tenantId") String tenantId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    // ── Reminder scheduler ────────────────────────────────────────────────────

    /**
     * Finds active (not cancelled / no-show) appointments scheduled between
     * {@code fromTime} and {@code toTime} on {@code date} that have not yet
     * had a reminder sent.  Used by the hourly reminder scheduler.
     */
    @Query("""
        SELECT a FROM Appointment a
        WHERE a.tenantId = :tenantId
          AND a.deleted = false
          AND a.reminderSent = false
          AND a.status NOT IN ('CANCELLED', 'NO_SHOW')
          AND a.scheduledDate = :date
          AND a.scheduledStartTime >= :fromTime
          AND a.scheduledStartTime < :toTime
        """)
    List<Appointment> findDueForReminder(
            @Param("tenantId") String tenantId,
            @Param("date") LocalDate date,
            @Param("fromTime") LocalTime fromTime,
            @Param("toTime") LocalTime toTime);

    // ── Employee conflict detection ───────────────────────────────────────────

    /**
     * Returns all active appointments on {@code date} where at least one
     * service is assigned to {@code employeeId}, excluding appointment
     * {@code excludeId} (pass -1 when creating a new appointment).
     * The overlap time check is done in the service layer in Java.
     */
    @Query("""
        SELECT DISTINCT a FROM Appointment a
        JOIN a.services s
        WHERE a.tenantId = :tenantId
          AND a.deleted = false
          AND a.status NOT IN ('CANCELLED', 'NO_SHOW')
          AND s.assignedEmployeeId = :employeeId
          AND a.scheduledDate = :date
          AND a.id <> :excludeId
        """)
    List<Appointment> findByEmployeeAndDate(
            @Param("tenantId") String tenantId,
            @Param("employeeId") Long employeeId,
            @Param("date") LocalDate date,
            @Param("excludeId") Long excludeId);

    // ── Analytics ─────────────────────────────────────────────────────────────

    /**
     * Returns Object[3]: [total, completedCount, cancelledCount]
     */
    @Query(value = """
        SELECT
          COUNT(*) AS total,
          SUM(CASE WHEN status = 'CHECKED_IN' THEN 1 ELSE 0 END) AS completed_count,
          SUM(CASE WHEN status IN ('CANCELLED', 'NO_SHOW') THEN 1 ELSE 0 END) AS cancelled_count
        FROM appointments
        WHERE scheduled_date BETWEEN :from AND :to
          AND deleted = false
          AND tenant_id = current_setting('app.current_tenant', true)
        """, nativeQuery = true)
    Object[] getAnalyticsSummary(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /** Trend by day: [label, total, completed, cancelled] */
    @Query(value = """
        SELECT
          TO_CHAR(scheduled_date, 'YYYY-MM-DD') AS label,
          COUNT(*) AS total,
          SUM(CASE WHEN status = 'CHECKED_IN' THEN 1 ELSE 0 END) AS completed,
          SUM(CASE WHEN status IN ('CANCELLED', 'NO_SHOW') THEN 1 ELSE 0 END) AS cancelled
        FROM appointments
        WHERE scheduled_date BETWEEN :from AND :to
          AND deleted = false
          AND tenant_id = current_setting('app.current_tenant', true)
        GROUP BY scheduled_date
        ORDER BY scheduled_date
        """, nativeQuery = true)
    List<Object[]> getAnalyticsTrendByDay(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /** Trend by week: [label (week-start), total, completed, cancelled] */
    @Query(value = """
        SELECT
          TO_CHAR(DATE_TRUNC('week', scheduled_date), 'YYYY-MM-DD') AS label,
          COUNT(*) AS total,
          SUM(CASE WHEN status = 'CHECKED_IN' THEN 1 ELSE 0 END) AS completed,
          SUM(CASE WHEN status IN ('CANCELLED', 'NO_SHOW') THEN 1 ELSE 0 END) AS cancelled
        FROM appointments
        WHERE scheduled_date BETWEEN :from AND :to
          AND deleted = false
          AND tenant_id = current_setting('app.current_tenant', true)
        GROUP BY DATE_TRUNC('week', scheduled_date)
        ORDER BY DATE_TRUNC('week', scheduled_date)
        """, nativeQuery = true)
    List<Object[]> getAnalyticsTrendByWeek(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /** Trend by month: [label (YYYY-MM), total, completed, cancelled] */
    @Query(value = """
        SELECT
          TO_CHAR(DATE_TRUNC('month', scheduled_date), 'YYYY-MM') AS label,
          COUNT(*) AS total,
          SUM(CASE WHEN status = 'CHECKED_IN' THEN 1 ELSE 0 END) AS completed,
          SUM(CASE WHEN status IN ('CANCELLED', 'NO_SHOW') THEN 1 ELSE 0 END) AS cancelled
        FROM appointments
        WHERE scheduled_date BETWEEN :from AND :to
          AND deleted = false
          AND tenant_id = current_setting('app.current_tenant', true)
        GROUP BY DATE_TRUNC('month', scheduled_date)
        ORDER BY DATE_TRUNC('month', scheduled_date)
        """, nativeQuery = true)
    List<Object[]> getAnalyticsTrendByMonth(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /** Top services by appointment count: [name, count] */
    @Query(value = """
        SELECT asi.product_name AS name, COUNT(DISTINCT a.id) AS cnt
        FROM appointments a
        JOIN appointment_services asi ON asi.appointment_id = a.id
        WHERE a.scheduled_date BETWEEN :from AND :to
          AND a.deleted = false
          AND a.tenant_id = current_setting('app.current_tenant', true)
        GROUP BY asi.product_name
        ORDER BY cnt DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> getServiceRanking(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("limit") int limit);

    /** Top employees by appointment count: [name, count] */
    @Query(value = """
        SELECT
          asi.assigned_employee_name AS name,
          COUNT(DISTINCT a.id) AS cnt
        FROM appointments a
        JOIN appointment_services asi ON asi.appointment_id = a.id
        WHERE a.scheduled_date BETWEEN :from AND :to
          AND a.deleted = false
          AND asi.assigned_employee_id IS NOT NULL
          AND a.tenant_id = current_setting('app.current_tenant', true)
        GROUP BY asi.assigned_employee_name, asi.assigned_employee_id
        ORDER BY cnt DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> getEmployeeRanking(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("limit") int limit);
}
