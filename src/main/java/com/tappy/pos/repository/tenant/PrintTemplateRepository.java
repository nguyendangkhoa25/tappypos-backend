package com.tappy.pos.repository.tenant;

import com.tappy.pos.model.entity.tenant.PrintTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrintTemplateRepository extends JpaRepository<PrintTemplate, Long> {

    List<PrintTemplate> findAllByTemplateTypeAndDeletedFalseOrderByIsDefaultDescNameAsc(String templateType);

    Optional<PrintTemplate> findFirstByTemplateTypeAndIsDefaultTrueAndDeletedFalse(String templateType);

    boolean existsByTemplateTypeAndDeletedFalse(String templateType);

    @Modifying
    @Query("UPDATE PrintTemplate t SET t.isDefault = false WHERE t.templateType = :type AND t.deleted = false")
    void clearDefaultForType(String type);
}
