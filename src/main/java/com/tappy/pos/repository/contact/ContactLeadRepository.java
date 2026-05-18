package com.tappy.pos.repository.contact;

import com.tappy.pos.model.entity.contact.ContactLead;
import com.tappy.pos.model.enums.LeadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactLeadRepository extends JpaRepository<ContactLead, Long> {

    @Query("SELECT COUNT(c) FROM ContactLead c WHERE c.deleted = false")
    long countAllActive();

    @Query("SELECT COUNT(c) FROM ContactLead c WHERE c.deleted = false AND c.status = :status")
    long countByStatus(@Param("status") LeadStatus status);

    @Query(value = "SELECT * FROM contact_leads WHERE deleted = false " +
            "AND (CAST(:status AS text) IS NULL OR status = CAST(:status AS text)) " +
            "ORDER BY created_at DESC",
            countQuery = "SELECT COUNT(*) FROM contact_leads WHERE deleted = false " +
            "AND (CAST(:status AS text) IS NULL OR status = CAST(:status AS text))",
            nativeQuery = true)
    Page<ContactLead> findAll(@Param("status") String status, Pageable pageable);
}
