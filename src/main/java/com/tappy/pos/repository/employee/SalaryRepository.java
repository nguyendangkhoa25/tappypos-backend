package com.tappy.pos.repository.employee;

import com.tappy.pos.model.entity.employee.Salary;
import com.tappy.pos.model.enums.SalaryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SalaryRepository extends JpaRepository<Salary, Long> {

    boolean existsByEmployeeIdAndMonthAndYear(Long employeeId, int month, int year);

    Optional<Salary> findByIdAndDeletedAtIsNull(Long id);

    @Query("""
            SELECT s FROM Salary s
            WHERE (:status IS NULL OR s.status = :status)
              AND (:year   IS NULL OR s.year  = :year)
              AND (:month  IS NULL OR s.month = :month)
              AND s.deletedAt IS NULL
            ORDER BY s.year DESC, s.month DESC, s.employeeName ASC
            """)
    Page<Salary> findAllFiltered(
            @Param("status") SalaryStatus status,
            @Param("year") Integer year,
            @Param("month") Integer month,
            Pageable pageable);

    @Query("""
            SELECT s FROM Salary s
            WHERE s.employeeId = :employeeId
              AND (:status IS NULL OR s.status = :status)
              AND (:year   IS NULL OR s.year  = :year)
              AND (:month  IS NULL OR s.month = :month)
              AND s.deletedAt IS NULL
            ORDER BY s.year DESC, s.month DESC
            """)
    Page<Salary> findByEmployeeIdFiltered(
            @Param("employeeId") Long employeeId,
            @Param("status") SalaryStatus status,
            @Param("year") Integer year,
            @Param("month") Integer month,
            Pageable pageable);
}
