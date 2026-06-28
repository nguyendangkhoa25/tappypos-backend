package com.tappy.pos.repository.order;

import com.tappy.pos.model.entity.order.Combo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComboRepository extends JpaRepository<Combo, Long> {
    List<Combo> findByDeletedFalse();
    List<Combo> findByDeletedFalseAndActive(Boolean active);
    Optional<Combo> findByIdAndDeletedFalse(Long id);
}
