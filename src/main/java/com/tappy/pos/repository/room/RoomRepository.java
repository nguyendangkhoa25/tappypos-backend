package com.tappy.pos.repository.room;

import com.tappy.pos.model.entity.room.RoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Tenant-scoped via PostgreSQL RLS (app.current_tenant). */
@Repository
public interface RoomRepository extends JpaRepository<RoomEntity, Long> {

    List<RoomEntity> findByDeletedFalseOrderBySortOrderAscRoomNumberAsc();

    Optional<RoomEntity> findByIdAndDeletedFalse(Long id);

    Optional<RoomEntity> findByQrTokenAndDeletedFalse(String qrToken);

    boolean existsByRoomNumberAndDeletedFalse(String roomNumber);
}
