package com.tappy.pos.repository.modifier;

import com.tappy.pos.model.entity.modifier.ModifierGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ModifierGroupRepository extends JpaRepository<ModifierGroup, Long> {

    List<ModifierGroup> findByDeletedFalseOrderBySortOrderAscIdAsc();

    Optional<ModifierGroup> findByIdAndDeletedFalse(Long id);
}
