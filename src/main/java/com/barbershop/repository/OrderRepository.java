package com.barbershop.repository;

import com.barbershop.model.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT o FROM Order o WHERE o.deletedAt IS NULL")
    Page<Order> findAllActive(Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.deletedAt IS NULL AND o.id = :id")
    Optional<Order> findByIdActive(Long id);

    @Query("SELECT o FROM Order o WHERE o.deletedAt IS NULL AND o.customer.id = :customerId")
    List<Order> findByCustomerId(Long customerId);

    @Query("SELECT o FROM Order o WHERE o.deletedAt IS NULL AND o.assignedEmployee.id = :employeeId")
    List<Order> findByEmployeeId(Long employeeId);

    @Query("SELECT o FROM Order o WHERE o.deletedAt IS NULL AND o.status = :status")
    Page<Order> findByStatus(@Param("status") String status, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.deletedAt IS NULL AND o.assignedEmployee.id = :employeeId " +
           "AND o.status = 'COMPLETED'")
    List<Order> findCompletedOrdersByEmployee(Long employeeId);

    @Query("SELECT o FROM Order o WHERE o.deletedAt IS NULL AND " +
           "(LOWER(o.customer.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.customer.phone) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Order> searchByCustomerKeyword(@Param("keyword") String keyword, Pageable pageable);
}

