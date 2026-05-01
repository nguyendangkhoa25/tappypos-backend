package com.knp.model.entity.tenant;

import com.knp.model.entity.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "shop_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ShopInfo extends TenantAwareEntity {

    @NotBlank(message = "Shop name is required")
    @Column(nullable = false)
    private String shopName;

    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String companyName;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(length = 150)
    private String supplierTaxCode;

    @Column(length = 200)
    private String website;
}
