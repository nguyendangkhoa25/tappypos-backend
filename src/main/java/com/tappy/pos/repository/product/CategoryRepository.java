package com.tappy.pos.repository.product;

import com.tappy.pos.model.entity.product.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByDeletedFalseOrderByName();
    List<Category> findByParentIdAndDeletedFalse(Long parentId);
    Optional<Category> findByIdAndDeletedFalse(Long id);

    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.parent WHERE c.deleted = false")
    List<Category> findAllActiveWithParent();
}

