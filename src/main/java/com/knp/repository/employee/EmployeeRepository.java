package com.knp.repository.employee;

import com.knp.model.entity.employee.Employee;
import com.knp.model.enums.EmployeePosition;
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

    @Query("SELECT e FROM Employee e WHERE e.deleted <> true " +
            "AND (:search IS NULL OR LOWER(e.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(e.phone) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(e.email) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY e.id DESC")
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
