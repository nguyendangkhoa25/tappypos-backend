package com.tappy.pos.repository.repair;

import com.tappy.pos.model.entity.repair.RepairTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface RepairTicketRepository extends JpaRepository<RepairTicket, Long> {

    Optional<RepairTicket> findByIdAndTenantIdAndDeletedFalse(Long id, String tenantId);

    // ── All tickets (REPAIR_VIEW_ALL present) ──────────────────────────────────
    @Query("""
        SELECT t FROM RepairTicket t
        WHERE t.tenantId = :tenantId
          AND t.deleted = false
          AND (:status IS NULL OR t.status = :status)
          AND (:keyword IS NULL OR LOWER(t.ticketNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(t.customerName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(t.customerPhone, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(t.serialImei, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
        ORDER BY t.createdAt DESC
        """)
    Page<RepairTicket> search(@Param("tenantId") String tenantId,
                              @Param("status") String status,
                              @Param("keyword") String keyword,
                              Pageable pageable);

    // ── Own tickets only (REPAIR_VIEW_ALL absent) ──────────────────────────────
    @Query("""
        SELECT t FROM RepairTicket t
        WHERE t.tenantId = :tenantId
          AND t.deleted = false
          AND t.createdBy = :createdBy
          AND (:status IS NULL OR t.status = :status)
          AND (:keyword IS NULL OR LOWER(t.ticketNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(t.customerName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(t.customerPhone, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(t.serialImei, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
        ORDER BY t.createdAt DESC
        """)
    Page<RepairTicket> searchByCreatedBy(@Param("tenantId") String tenantId,
                                         @Param("status") String status,
                                         @Param("keyword") String keyword,
                                         @Param("createdBy") String createdBy,
                                         Pageable pageable);

    @Query("""
        SELECT COUNT(t) + 1 FROM RepairTicket t
        WHERE t.tenantId = :tenantId
          AND CAST(t.createdAt AS LocalDate) = :today
        """)
    long countTodayByTenantId(@Param("tenantId") String tenantId, @Param("today") LocalDate today);

    @Query("""
        SELECT t.status, COUNT(t) FROM RepairTicket t
        WHERE t.tenantId = :tenantId AND t.deleted = false
        GROUP BY t.status
        """)
    java.util.List<Object[]> countByStatus(@Param("tenantId") String tenantId);
}
