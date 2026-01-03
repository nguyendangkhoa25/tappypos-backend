package com.barbershop.repository;

import com.barbershop.model.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    @Query("SELECT o FROM Order o WHERE o.deleted = false")
    Page<Order> findAllActive(Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.deleted = false AND o.id = :id")
    Optional<Order> findByIdActive(Long id);

    @Query("SELECT o FROM Order o WHERE o.deleted = false AND o.customer.id = :customerId")
    List<Order> findByCustomerId(Long customerId);


    @Query("SELECT o FROM Order o WHERE o.deleted = false AND o.status = :status")
    Page<Order> findByStatus(@Param("status") String status, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.deleted = false AND o.status = :status")
    Page<Order> findByStatusActive(@Param("status") Order.OrderStatus status, Pageable pageable);


    @Query("SELECT o FROM Order o WHERE o.deleted = false AND " +
           "(LOWER(o.customer.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.customer.phone) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Order> searchByCustomerKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.deleted = false AND o.status = 'COMPLETED' AND " +
           "o.completedAt >= :startDate AND o.completedAt <= :endDate")
    List<Order> findByCompletedAtBetweenAndStatus(@Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate,
                                                   @Param("status") String status);

    @Query("SELECT o FROM Order o WHERE o.deleted = false AND o.status = 'COMPLETED' AND " +
           "YEAR(o.completedAt) = :year AND MONTH(o.completedAt) = :month")
    List<Order> findCompletedOrdersByYearAndMonth(@Param("year") Integer year, @Param("month") Integer month);
}

