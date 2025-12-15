package com.barbershop.repository;

import com.barbershop.model.entity.Employee;
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

    @Query("SELECT e FROM Employee e WHERE e.deletedAt IS NULL")
    Page<Employee> findAllActive(Pageable pageable);

    @Query("SELECT e FROM Employee e WHERE e.deletedAt IS NULL AND e.id = :id")
    Optional<Employee> findByIdActive(Long id);

    @Query("SELECT e FROM Employee e WHERE e.deletedAt IS NULL AND e.phone = :phone")
    Optional<Employee> findByPhone(String phone);

    @Query("SELECT e FROM Employee e WHERE e.deletedAt IS NULL AND e.status = :status")
    List<Employee> findByStatus(String status);

    @Query("SELECT e FROM Employee e WHERE e.deletedAt IS NULL AND " +
           "(LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.phone) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Employee> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}

