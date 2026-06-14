package com.tappy.pos.repository.booking;

import com.tappy.pos.model.entity.booking.BookingResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookingResourceRepository extends JpaRepository<BookingResource, Long> {

    Optional<BookingResource> findByIdAndTenantIdAndDeletedFalse(Long id, String tenantId);

    @Query("""
        SELECT r FROM BookingResource r
        WHERE r.tenantId = :tenantId
          AND r.deleted = false
        ORDER BY r.sortOrder ASC, r.id ASC
        """)
    List<BookingResource> findAllActive(@Param("tenantId") String tenantId);
}
