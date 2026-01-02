package com.barbershop.repository;

import com.barbershop.model.entity.Revenue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RevenueRepository extends JpaRepository<Revenue, Long> {

    @Query("SELECT r FROM Revenue r WHERE r.year = :year AND r.month = :month AND r.deleted = false")
    Optional<Revenue> findByYearAndMonth(@Param("year") Integer year, @Param("month") Integer month);

    @Query("SELECT r FROM Revenue r WHERE r.deleted = false ORDER BY r.year DESC, r.month DESC")
    Page<Revenue> findAllNotDeleted(Pageable pageable);

    @Query("SELECT r FROM Revenue r WHERE r.year = :year AND r.deleted = false ORDER BY r.month DESC")
    Page<Revenue> findByYearNotDeleted(@Param("year") Integer year, Pageable pageable);

    @Query("SELECT r FROM Revenue r WHERE r.year = :year AND r.month = :month AND r.deleted = false")
    Optional<Revenue> findByYearAndMonthNotDeleted(@Param("year") Integer year, @Param("month") Integer month);
}

