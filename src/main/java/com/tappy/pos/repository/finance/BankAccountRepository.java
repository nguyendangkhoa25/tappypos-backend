package com.tappy.pos.repository.finance;

import com.tappy.pos.model.entity.finance.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    @Query("SELECT b FROM BankAccount b WHERE b.deleted = false ORDER BY b.isDefault DESC, b.id ASC")
    List<BankAccount> findAllActive();

    @Query("SELECT b FROM BankAccount b WHERE b.deleted = false AND b.isDefault = true ORDER BY b.id ASC")
    Optional<BankAccount> findDefault();

    @Modifying
    @Query("UPDATE BankAccount b SET b.isDefault = false WHERE b.deleted = false AND b.id <> :id")
    void clearOtherDefaults(@Param("id") Long id);
}
