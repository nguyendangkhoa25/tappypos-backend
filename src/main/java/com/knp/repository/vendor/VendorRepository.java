package com.knp.repository.vendor;

import com.knp.model.entity.vendor.Vendor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {

    @Query("SELECT v FROM Vendor v WHERE v.deleted = false ORDER BY v.name ASC")
    Page<Vendor> findAllActive(Pageable pageable);

    @Query("SELECT v FROM Vendor v WHERE v.deleted = false AND (LOWER(v.name) LIKE LOWER(CONCAT('%',:kw,'%')) OR LOWER(v.code) LIKE LOWER(CONCAT('%',:kw,'%')))")
    Page<Vendor> search(@Param("kw") String keyword, Pageable pageable);

    @Query("SELECT v FROM Vendor v WHERE v.deleted = false AND v.code = :code")
    Optional<Vendor> findByCode(@Param("code") String code);

    @Query("SELECT v FROM Vendor v WHERE v.deleted = false AND v.isActive = true ORDER BY v.name ASC")
    java.util.List<Vendor> findAllActiveForSelect();
}
