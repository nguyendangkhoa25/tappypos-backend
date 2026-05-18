package com.tappy.pos.model.dto.vendor;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class VendorDTO {
    private Long id;
    private String name;
    private String code;
    private String contactName;
    private String email;
    private String phone;
    private String address;
    private String taxId;
    private String paymentTerms;
    private Boolean isActive;
    private String notes;
    private LocalDateTime createdAt;
}
