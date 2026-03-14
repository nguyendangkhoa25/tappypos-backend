package com.knp.repository;

import com.knp.model.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByDeletedFalseOrderByName();
    List<Category> findByParentIdAndDeletedFalse(Long parentId);
    Optional<Category> findByIdAndDeletedFalse(Long id);
}

