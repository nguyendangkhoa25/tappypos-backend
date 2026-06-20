package com.tappy.pos.repository.booking;

import com.tappy.pos.model.entity.booking.BookingResourceRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookingResourceRateRepository extends JpaRepository<BookingResourceRate, Long> {

    @Query("""
        SELECT r FROM BookingResourceRate r
        WHERE r.tenantId = :tenantId
          AND r.deleted = false
        ORDER BY r.resourceId ASC, r.sortOrder ASC, r.id ASC
        """)
    List<BookingResourceRate> findAllActive(@Param("tenantId") String tenantId);

    @Query("""
        SELECT r FROM BookingResourceRate r
        WHERE r.tenantId = :tenantId
          AND r.resourceId = :resourceId
          AND r.deleted = false
        ORDER BY r.sortOrder ASC, r.id ASC
        """)
    List<BookingResourceRate> findByResource(@Param("tenantId") String tenantId,
                                             @Param("resourceId") Long resourceId);

    /** Replace-all helper: soft-delete the existing windows of a resource before re-inserting. */
    @Modifying
    @Query("""
        UPDATE BookingResourceRate r SET r.deleted = true
        WHERE r.tenantId = :tenantId AND r.resourceId = :resourceId AND r.deleted = false
        """)
    void softDeleteByResource(@Param("tenantId") String tenantId, @Param("resourceId") Long resourceId);
}
