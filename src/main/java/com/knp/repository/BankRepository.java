package com.knp.repository;

import com.knp.model.entity.Bank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankRepository extends JpaRepository<Bank, Long> {

    @Query("SELECT b FROM Bank b WHERE b.deleted = false AND b.isActive = true ORDER BY b.sortOrder ASC, b.name ASC")
    List<Bank> findAllActiveOrderBySortOrder();
}
