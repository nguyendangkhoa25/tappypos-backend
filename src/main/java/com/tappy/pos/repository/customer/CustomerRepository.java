package com.tappy.pos.repository.customer;

import com.tappy.pos.model.entity.customer.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Query("SELECT c FROM Customer c WHERE c.deleted = false")
    Page<Customer> findAllActive(Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.deleted = false AND c.id = :id")
    Optional<Customer> findByIdActive(Long id);

    // Explicit tenant scoping — does not rely on the Hibernate filter / RLS being active
    @Query("SELECT c FROM Customer c WHERE c.deleted = false AND c.id = :id AND c.tenantId = :tenantId")
    Optional<Customer> findByIdActiveAndTenantId(@Param("id") Long id, @Param("tenantId") String tenantId);

    @Query("SELECT c FROM Customer c WHERE c.deleted = false AND c.phone = :phone")
    Optional<Customer> findByPhone(String phone);

    @Query("SELECT c FROM Customer c WHERE c.deleted = false AND c.email = :email")
    Optional<Customer> findByEmail(String email);

    @Query("SELECT c FROM Customer c WHERE c.deleted = false AND " +
           "(LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.phone) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Customer> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.deleted = false AND c.name = :name")
    Optional<Customer> findByName(@Param("name") String name);

    // Count query for dashboard optimization
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.deleted = false")
    long countAllActive();

    // Provisioning helper — explicit tenant_id avoids relying on Hibernate filter state
    @Query("SELECT c FROM Customer c WHERE c.deleted = false AND c.phone = :phone AND c.tenantId = :tenantId")
    Optional<Customer> findByPhoneAndTenantId(@Param("phone") String phone, @Param("tenantId") String tenantId);

    @Query("SELECT c FROM Customer c WHERE c.deleted = false ORDER BY c.createdAt DESC")
    java.util.List<Customer> findTop(org.springframework.data.domain.Pageable pageable);

    // Analytics: new customers created within a date range (excludes walk-in phone)
    @Query(value = "SELECT COUNT(id) FROM customers WHERE deleted = false AND phone != '0000000000' " +
                   "AND created_at BETWEEN :from AND :to " +
                   "AND tenant_id = current_setting('app.current_tenant', true)", nativeQuery = true)
    long countNewInPeriod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /**
     * Customers whose birthday falls in {@code month} (1=Jan … 12=Dec),
     * ordered by day-of-month ascending so the result is sorted within the month.
     * The client re-orders to put today / upcoming days before already-passed days.
     */
    @Query("SELECT c FROM Customer c WHERE c.deleted = false " +
           "AND c.dateOfBirth IS NOT NULL " +
           "AND MONTH(c.dateOfBirth) = :month " +
           "ORDER BY DAY(c.dateOfBirth) ASC")
    java.util.List<Customer> findByBirthdayMonth(@Param("month") int month);
}
