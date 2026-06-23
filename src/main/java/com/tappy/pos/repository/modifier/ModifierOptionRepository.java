package com.tappy.pos.repository.modifier;

import com.tappy.pos.model.entity.modifier.ModifierOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ModifierOptionRepository extends JpaRepository<ModifierOption, Long> {

    @Query("SELECT o FROM ModifierOption o JOIN FETCH o.modifierGroup " +
           "WHERE o.id IN :ids AND o.deleted = false")
    List<ModifierOption> findAllByIdInWithGroup(@Param("ids") Collection<Long> ids);
}
