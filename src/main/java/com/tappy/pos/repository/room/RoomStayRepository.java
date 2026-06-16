package com.tappy.pos.repository.room;

import com.tappy.pos.model.entity.room.RoomStayEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Tenant-scoped via PostgreSQL RLS (app.current_tenant). */
@Repository
public interface RoomStayRepository extends JpaRepository<RoomStayEntity, Long> {

    Optional<RoomStayEntity> findByIdAndDeletedFalse(Long id);

    /** The current in-house stay for a room, if any. */
    Optional<RoomStayEntity> findFirstByRoomIdAndStatusAndDeletedFalse(Long roomId, String status);

    Page<RoomStayEntity> findByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    Page<RoomStayEntity> findByStatusAndDeletedFalseOrderByCreatedAtDesc(String status, Pageable pageable);

    long countByStatusAndDeletedFalse(String status);
}
