package com.knp.model.dto.vendor;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SaveVendorRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String code;
    private String contactName;
    private String email;
    private String phone;
    private String address;
    private String taxId;
    private String paymentTerms;
    private Boolean isActive;
    private String notes;
}
