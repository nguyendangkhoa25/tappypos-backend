package com.tappy.pos.repository.employee;

import com.tappy.pos.model.entity.employee.Employee;
import com.tappy.pos.model.enums.EmployeePosition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    @Query(value = "SELECT * FROM employees WHERE deleted <> true " +
            "AND (CAST(:search AS text) IS NULL OR LOWER(full_name) LIKE LOWER('%' || CAST(:search AS text) || '%') " +
            "OR LOWER(phone) LIKE LOWER('%' || CAST(:search AS text) || '%') " +
            "OR LOWER(email) LIKE LOWER('%' || CAST(:search AS text) || '%')) " +
            "ORDER BY id DESC",
           countQuery = "SELECT COUNT(*) FROM employees WHERE deleted <> true " +
            "AND (CAST(:search AS text) IS NULL OR LOWER(full_name) LIKE LOWER('%' || CAST(:search AS text) || '%') " +
            "OR LOWER(phone) LIKE LOWER('%' || CAST(:search AS text) || '%') " +
            "OR LOWER(email) LIKE LOWER('%' || CAST(:search AS text) || '%'))",
           nativeQuery = true)
    Page<Employee> findAllWithSearch(@Param("search") String search, Pageable pageable);

    @Query("SELECT e FROM Employee e WHERE e.deleted <> true ORDER BY e.id DESC")
    List<Employee> findAllActive();

    @Query("SELECT e FROM Employee e WHERE e.deleted <> true AND e.active = true ORDER BY e.id DESC")
    List<Employee> findAllActiveAndEnabled();

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.deleted <> true")
    long countAllActive();

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.deleted <> true AND e.active = true")
    long countActive();

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.deleted <> true AND e.active = false")
    long countInactive();

    Optional<Employee> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    @Query("SELECT e FROM Employee e WHERE e.deleted <> true AND e.position = :position ORDER BY e.id DESC")
    List<Employee> findByPosition(@Param("position") EmployeePosition position);
}
