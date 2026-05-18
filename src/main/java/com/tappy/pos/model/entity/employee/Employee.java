package com.tappy.pos.model.entity.employee;

import com.tappy.pos.model.enums.EmployeePosition;
import jakarta.persistence.*;
import com.tappy.pos.model.entity.TenantAwareEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "employees")
public class Employee extends TenantAwareEntity {

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "nick_name", length = 100)
    private String nickName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "position")
    private EmployeePosition position;

    @Column(name = "department")
    private String department;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "base_wage", precision = 15, scale = 2)
    private BigDecimal baseWage;

    @Column(name = "commission_rate", precision = 5, scale = 2)
    private BigDecimal commissionRate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "avatar")
    private String avatar;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "id_card_number", length = 20)
    private String idCardNumber;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "permanent_address", columnDefinition = "TEXT")
    private String permanentAddress;

    @Column(name = "id_card_issued_date")
    private LocalDate idCardIssuedDate;

    @Column(name = "id_card_issued_place")
    private String idCardIssuedPlace;

    @Column(name = "id_card_front_image", columnDefinition = "TEXT")
    private String idCardFrontImage;

    @Column(name = "id_card_back_image", columnDefinition = "TEXT")
    private String idCardBackImage;
}
