package com.tappy.pos.repository.employee;

import com.tappy.pos.model.entity.employee.SalaryAdvance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface SalaryAdvanceRepository extends JpaRepository<SalaryAdvance, Long> {

    @Query("""
            SELECT a FROM SalaryAdvance a
            WHERE (:employeeId IS NULL OR a.employeeId = :employeeId)
              AND a.deletedAt IS NULL
            ORDER BY a.advanceDate DESC, a.id DESC
            """)
    Page<SalaryAdvance> findFiltered(@Param("employeeId") Long employeeId, Pageable pageable);

    @Query(value = """
            SELECT COALESCE(SUM(a.amount), 0)
            FROM salary_advance a
            WHERE a.employee_id = :employeeId
              AND a.is_deducted = false
              AND a.salary_id IS NULL
              AND EXTRACT(YEAR  FROM a.advance_date) = :year
              AND EXTRACT(MONTH FROM a.advance_date) = :month
            """, nativeQuery = true)
    BigDecimal sumPendingByEmployeeAndMonth(
            @Param("employeeId") Long employeeId,
            @Param("month") int month,
            @Param("year") int year);

    @Modifying
    @Query(value = """
            UPDATE salary_advance
            SET salary_id = :salaryId, is_deducted = true
            WHERE employee_id = :employeeId
              AND is_deducted = false
              AND salary_id IS NULL
              AND EXTRACT(YEAR  FROM advance_date) = :year
              AND EXTRACT(MONTH FROM advance_date) = :month
            """, nativeQuery = true)
    int linkAdvancesToSalary(
            @Param("salaryId") Long salaryId,
            @Param("employeeId") Long employeeId,
            @Param("month") int month,
            @Param("year") int year);

    @Modifying
    @Query(value = "UPDATE salary_advance SET salary_id = NULL, is_deducted = false WHERE salary_id = :salaryId",
           nativeQuery = true)
    int unlinkFromSalary(@Param("salaryId") Long salaryId);
}
