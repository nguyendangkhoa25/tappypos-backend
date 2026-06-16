package com.tappy.pos.repository.room;

import com.tappy.pos.model.entity.room.RoomStayItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Tenant-scoped via PostgreSQL RLS (app.current_tenant). */
@Repository
public interface RoomStayItemRepository extends JpaRepository<RoomStayItemEntity, Long> {

    List<RoomStayItemEntity> findByStayIdAndDeletedFalseOrderByCreatedAtAsc(Long stayId);

    Optional<RoomStayItemEntity> findByIdAndDeletedFalse(Long id);
}
