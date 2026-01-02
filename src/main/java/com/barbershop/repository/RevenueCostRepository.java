package com.barbershop.repository;

import com.barbershop.model.entity.RevenueCost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RevenueCostRepository extends JpaRepository<RevenueCost, Long> {

    @Query("SELECT rc FROM RevenueCost rc WHERE rc.revenue.id = :revenueId AND rc.deleted = false")
    List<RevenueCost> findByRevenueId(@Param("revenueId") Long revenueId);

    @Query("DELETE FROM RevenueCost rc WHERE rc.revenue.id = :revenueId")
    void deleteByRevenueId(@Param("revenueId") Long revenueId);
}

