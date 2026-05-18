package com.tappy.pos.model.entity.appointment;

import com.tappy.pos.model.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Appointment extends TenantAwareEntity {

    @Column(name = "appointment_number", nullable = false, length = 20)
    private String appointmentNumber;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_name", nullable = false, length = 255)
    private String customerName;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "scheduled_start_time", nullable = false)
    private LocalTime scheduledStartTime;

    @Builder.Default
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes = 60;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "linked_order_id")
    private Long linkedOrderId;

    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    @Builder.Default
    @OneToMany(mappedBy = "appointment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AppointmentServiceItem> services = new ArrayList<>();
}
