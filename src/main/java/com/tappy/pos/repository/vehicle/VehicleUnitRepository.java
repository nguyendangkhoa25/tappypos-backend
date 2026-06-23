package com.tappy.pos.repository.vehicle;

import com.tappy.pos.model.entity.vehicle.VehicleUnitEntity;
import com.tappy.pos.model.enums.VehicleUnitStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleUnitRepository extends JpaRepository<VehicleUnitEntity, Long> {

    Optional<VehicleUnitEntity> findByIdAndDeletedFalse(Long id);

    @Query("SELECT v FROM VehicleUnitEntity v WHERE v.deleted = false " +
            "AND (:status IS NULL OR v.status = :status) " +
            "AND (:productId IS NULL OR v.productId = :productId) " +
            "ORDER BY v.createdAt DESC")
    Page<VehicleUnitEntity> search(@Param("status") VehicleUnitStatus status,
                                   @Param("productId") Long productId, Pageable pageable);

    /** "Tra cứu xe theo số khung / số máy / biển số" — case-insensitive contains across all three. */
    @Query("SELECT v FROM VehicleUnitEntity v WHERE v.deleted = false AND (" +
            "LOWER(v.frameNo) LIKE LOWER(CONCAT('%', :kw, '%')) OR " +
            "LOWER(v.engineNo) LIKE LOWER(CONCAT('%', :kw, '%')) OR " +
            "LOWER(v.licensePlate) LIKE LOWER(CONCAT('%', :kw, '%'))) " +
            "ORDER BY v.createdAt DESC")
    List<VehicleUnitEntity> lookup(@Param("kw") String keyword);

    boolean existsByFrameNoAndTenantIdAndDeletedFalse(String frameNo, String tenantId);

    boolean existsByEngineNoAndTenantIdAndDeletedFalse(String engineNo, String tenantId);
}
