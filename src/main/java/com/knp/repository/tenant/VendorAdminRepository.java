package com.knp.repository.tenant;

import com.knp.model.entity.tenant.VendorAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VendorAdminRepository extends JpaRepository<VendorAdmin, Long> {

    @Query("SELECT v FROM VendorAdmin v WHERE v.deleted = false AND v.active = true AND " +
           "(:search IS NULL OR LOWER(v.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<VendorAdmin> findAllActive(@Param("search") String search);

    boolean existsByName(String name);

    @Query("SELECT v FROM VendorAdmin v WHERE v.deleted = false AND v.userId = :userId")
    java.util.Optional<VendorAdmin> findByUserId(@Param("userId") Long userId);
}
