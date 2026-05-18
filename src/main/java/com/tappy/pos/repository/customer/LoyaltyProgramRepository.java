package com.tappy.pos.repository.customer;

import com.tappy.pos.model.entity.customer.LoyaltyProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoyaltyProgramRepository extends JpaRepository<LoyaltyProgram, Long> {

    @Query("SELECT p FROM LoyaltyProgram p WHERE p.deleted = false ORDER BY p.id ASC LIMIT 1")
    Optional<LoyaltyProgram> findActiveProgram();
}
