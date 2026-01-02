package com.barbershop.repository;

import com.barbershop.model.entity.Salary;
import com.barbershop.model.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalaryRepository extends JpaRepository<Salary, Long> {

    /**
     * Find salary by employee, month, and year
     */
    @Query("SELECT s FROM Salary s WHERE s.deleted = false AND s.employee.id = :employeeId AND s.month = :month AND s.year = :year")
    Optional<Salary> findByEmployeeAndMonthAndYear(@Param("employeeId") Long employeeId, @Param("month") Integer month, @Param("year") Integer year);

    /**
     * Find all salaries for an employee in a given year
     */
    @Query("SELECT s FROM Salary s WHERE s.deleted = false AND s.employee.id = :employeeId AND s.year = :year ORDER BY s.month DESC")
    List<Salary> findByEmployeeAndYear(@Param("employeeId") Long employeeId, @Param("year") Integer year);

    /**
     * Find all salaries for an employee with pagination
     */
    @Query("SELECT s FROM Salary s WHERE s.deleted = false AND s.employee.id = :employeeId ORDER BY s.year DESC, s.month DESC")
    Page<Salary> findByEmployee(@Param("employeeId") Long employeeId, Pageable pageable);

    /**
     * Find salaries within a date range
     */
    @Query("SELECT s FROM Salary s WHERE s.deleted = false AND s.employee.id = :employeeId AND s.createdAt >= :startDate AND s.createdAt <= :endDate")
    List<Salary> findByEmployeeAndDateRange(@Param("employeeId") Long employeeId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Find all salaries with pagination
     */
    @Query("SELECT s FROM Salary s WHERE s.deleted = false ORDER BY s.year DESC, s.month DESC")
    Page<Salary> findAllActive(Pageable pageable);

    /**
     * Find all salaries for a specific month and year
     */
    @Query("SELECT s FROM Salary s WHERE s.deleted = false AND s.month = :month AND s.year = :year")
    List<Salary> findByYearAndMonthAndDeletedFalse(@Param("year") Integer year, @Param("month") Integer month);

    /**
     * Find salaries by status with pagination
     */
    @Query("SELECT s FROM Salary s WHERE s.deleted = false AND s.status = :status ORDER BY s.year DESC, s.month DESC")
    Page<Salary> findByStatus(@Param("status") Salary.SalaryStatus status, Pageable pageable);

    /**
     * Find salaries by employee and status
     */
    @Query("SELECT s FROM Salary s WHERE s.deleted = false AND s.employee.id = :employeeId AND s.status = :status ORDER BY s.year DESC, s.month DESC")
    Page<Salary> findByEmployeeAndStatus(@Param("employeeId") Long employeeId, @Param("status") Salary.SalaryStatus status, Pageable pageable);

    /**
     * Check if salary already exists for employee in given month/year
     */
    @Query("SELECT COUNT(s) > 0 FROM Salary s WHERE s.deleted = false AND s.employee.id = :employeeId AND s.month = :month AND s.year = :year")
    boolean existsByEmployeeAndMonthAndYear(@Param("employeeId") Long employeeId, @Param("month") Integer month, @Param("year") Integer year);
}

