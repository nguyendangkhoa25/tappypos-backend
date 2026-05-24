package com.tappy.pos.repository.tenant;

import com.tappy.pos.model.entity.tenant.ZaloMessageTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ZaloMessageTemplateRepository extends JpaRepository<ZaloMessageTemplate, Long> {

    List<ZaloMessageTemplate> findAllByTemplateTypeAndDeletedFalseOrderByIsDefaultDescNameAsc(String templateType);

    Optional<ZaloMessageTemplate> findFirstByTemplateTypeAndIsDefaultTrueAndDeletedFalse(String templateType);

    boolean existsByTemplateTypeAndDeletedFalse(String templateType);

    @Modifying
    @Query("UPDATE ZaloMessageTemplate t SET t.isDefault = false WHERE t.templateType = :type AND t.deleted = false")
    void clearDefaultForType(@Param("type") String type);
}
