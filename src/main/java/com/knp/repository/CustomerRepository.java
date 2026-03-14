package com.knp.repository;

import com.knp.model.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Query("SELECT c FROM Customer c WHERE c.deleted = false")
    Page<Customer> findAllActive(Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.deleted = false AND c.id = :id")
    Optional<Customer> findByIdActive(Long id);

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
}
