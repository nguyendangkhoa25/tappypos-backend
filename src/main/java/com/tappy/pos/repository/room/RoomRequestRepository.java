package com.tappy.pos.repository.room;

import com.tappy.pos.model.entity.room.RoomRequestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Tenant-scoped via PostgreSQL RLS (app.current_tenant). */
@Repository
public interface RoomRequestRepository extends JpaRepository<RoomRequestEntity, Long> {

    Optional<RoomRequestEntity> findByIdAndDeletedFalse(Long id);

    Page<RoomRequestEntity> findByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    Page<RoomRequestEntity> findByStatusAndDeletedFalseOrderByCreatedAtDesc(String status, Pageable pageable);

    long countByStatusAndDeletedFalse(String status);
}
