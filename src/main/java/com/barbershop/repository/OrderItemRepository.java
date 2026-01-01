package com.barbershop.repository;

import com.barbershop.model.entity.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.order o WHERE oi.id = :id")
    Optional<OrderItem> findByIdWithOrder(Long id);

    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.order o WHERE o.deleted = false")
    List<OrderItem> findAllActive();

    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.order o WHERE o.deleted = false")
    Page<OrderItem> findAllActive(Pageable pageable);

    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.order o WHERE o.deleted = false " +
           "AND oi.assignedEmployee.id = :employeeId")
    List<OrderItem> findByAssignedEmployeeId(@Param("employeeId") Long employeeId);

    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.order o WHERE o.deleted = false " +
           "AND oi.assignedEmployee.id = :employeeId")
    Page<OrderItem> findByAssignedEmployeeId(@Param("employeeId") Long employeeId, Pageable pageable);

    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.order o WHERE o.deleted = false " +
           "AND oi.status = :status")
    List<OrderItem> findByStatus(@Param("status") OrderItem.ItemStatus status);

    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.order o WHERE o.deleted = false " +
           "AND oi.status = :status")
    Page<OrderItem> findByStatus(@Param("status") OrderItem.ItemStatus status, Pageable pageable);

    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.order o WHERE o.deleted = false " +
           "AND oi.assignedEmployee.id = :employeeId AND oi.status = :status")
    List<OrderItem> findByAssignedEmployeeIdAndStatus(@Param("employeeId") Long employeeId,
                                                      @Param("status") OrderItem.ItemStatus status);

    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.order o WHERE o.deleted = false " +
           "AND oi.assignedEmployee.id = :employeeId AND oi.status = :status")
    Page<OrderItem> findByAssignedEmployeeIdAndStatus(@Param("employeeId") Long employeeId,
                                                      @Param("status") OrderItem.ItemStatus status,
                                                      Pageable pageable);

    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.order o WHERE o.deleted = false " +
           "AND o.id = :orderId")
    List<OrderItem> findByOrderId(@Param("orderId") Long orderId);

    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.order o WHERE o.deleted = false " +
           "AND oi.status = :status AND o.id = :orderId")
    List<OrderItem> findByOrderIdAndStatus(@Param("orderId") Long orderId,
                                          @Param("status") OrderItem.ItemStatus status);

    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.order o WHERE o.deleted = false " +
           "AND oi.assignedEmployee.id = :employeeId " +
           "AND oi.status = 'COMPLETED' " +
           "AND oi.salaryCalculated = false " +
           "AND oi.completedAt IS NOT NULL " +
           "AND MONTH(oi.completedAt) = :month " +
           "AND YEAR(oi.completedAt) = :year")
    List<OrderItem> findUncalculatedItemsByEmployeeAndMonthYear(
            @Param("employeeId") Long employeeId,
            @Param("month") Integer month,
            @Param("year") Integer year);

    // NEW: Flexible query with date range and salary calculated flag filtering
    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.order o WHERE o.deleted = false " +
           "AND oi.assignedEmployee.id = :employeeId " +
           "AND oi.status = 'COMPLETED' " +
           "AND oi.completedAt IS NOT NULL " +
           "AND oi.completedAt >= :fromDate " +
           "AND oi.completedAt < :toDate " +
           "AND oi.salaryCalculated = :salaryCalculated " +
           "ORDER BY oi.completedAt DESC")
    Page<OrderItem> findByEmployeeAndDateRangeAndSalaryCalculated(
            @Param("employeeId") Long employeeId,
            @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toDate") java.time.LocalDateTime toDate,
            @Param("salaryCalculated") Boolean salaryCalculated,
            Pageable pageable);

    // Query to get both calculated and uncalculated items in a date range
    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.order o WHERE o.deleted = false " +
           "AND oi.assignedEmployee.id = :employeeId " +
           "AND oi.status = 'COMPLETED' " +
           "AND oi.completedAt IS NOT NULL " +
           "AND oi.completedAt >= :fromDate " +
           "AND oi.completedAt < :toDate " +
           "ORDER BY oi.completedAt DESC")
    Page<OrderItem> findByEmployeeAndDateRange(
            @Param("employeeId") Long employeeId,
            @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toDate") java.time.LocalDateTime toDate,
            Pageable pageable);

    // Get uncalculated items for an employee, paginated and sorted by completed date DESC
    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.order o WHERE o.deleted = false " +
           "AND oi.assignedEmployee.id = :employeeId " +
           "AND oi.status = 'COMPLETED' " +
           "AND oi.salaryCalculated = false " +
           "AND oi.completedAt IS NOT NULL " +
           "ORDER BY oi.completedAt DESC")
    Page<OrderItem> findUncalculatedItemsByEmployeePagedAndSorted(
            @Param("employeeId") Long employeeId,
            Pageable pageable);

    @Query("SELECT oi FROM OrderItem oi WHERE oi.includedInSalary.id = :salaryId")
    List<OrderItem> findItemsBySalaryId(@Param("salaryId") Long salaryId);
}

