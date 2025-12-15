package com.barbershop.repository;

import com.barbershop.model.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Query("SELECT c FROM Customer c WHERE c.deletedAt IS NULL")
    Page<Customer> findAllActive(Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.deletedAt IS NULL AND c.id = :id")
    Optional<Customer> findByIdActive(Long id);

    @Query("SELECT c FROM Customer c WHERE c.deletedAt IS NULL AND c.phone = :phone")
    Optional<Customer> findByPhone(String phone);

    @Query("SELECT c FROM Customer c WHERE c.deletedAt IS NULL AND c.email = :email")
    Optional<Customer> findByEmail(String email);

    @Query("SELECT c FROM Customer c WHERE c.deletedAt IS NULL AND " +
           "(LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.phone) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Customer> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}

