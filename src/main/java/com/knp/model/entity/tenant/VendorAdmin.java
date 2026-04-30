package com.knp.model.entity.tenant;

import com.knp.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity(name = "VendorAdmin")
@Table(name = "vendors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorAdmin extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "user_id")
    private Long userId;
}
