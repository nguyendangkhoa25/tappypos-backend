package com.knp.repository;

import com.knp.model.entity.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductTypeRepository extends JpaRepository<ProductType, Long> {
    Optional<ProductType> findByCode(String code);
    Optional<ProductType> findByIdAndDeletedFalse(Long id);
}

