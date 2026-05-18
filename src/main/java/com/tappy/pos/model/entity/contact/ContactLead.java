package com.tappy.pos.model.entity.contact;

import com.tappy.pos.model.entity.BaseEntity;
import com.tappy.pos.model.enums.LeadStatus;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "contact_leads")
public class ContactLead extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "shop_type", length = 50)
    private String shopType;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "source", length = 50)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private LeadStatus status = LeadStatus.NEW;

    @Column(name = "admin_note", length = 1000)
    private String adminNote;
}
