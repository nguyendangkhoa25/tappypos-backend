package com.tappy.pos.repository.installment;

import com.tappy.pos.model.entity.installment.InstallmentScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InstallmentScheduleRepository extends JpaRepository<InstallmentScheduleEntity, Long> {

    Optional<InstallmentScheduleEntity> findByIdAndTenantIdAndDeletedFalse(Long id, String tenantId);

    List<InstallmentScheduleEntity> findByDebtIdAndDeletedFalseOrderByInstallmentNoAsc(Long debtId);

    /** Overdue, still-unpaid kỳ across the current tenant (RLS-scoped) for the reminder job. */
    @Query("SELECT s FROM InstallmentScheduleEntity s WHERE s.deleted = false AND s.paid = false " +
           "AND s.dueDate < :today ORDER BY s.dueDate ASC")
    List<InstallmentScheduleEntity> findOverdue(@Param("today") LocalDate today);

    void deleteByDebtId(Long debtId);
}
