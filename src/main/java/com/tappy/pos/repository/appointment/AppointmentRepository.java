package com.tappy.pos.repository.appointment;

import com.tappy.pos.model.entity.appointment.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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
}
