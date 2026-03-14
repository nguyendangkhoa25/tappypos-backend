package com.knp.model.dto.bank;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SaveBankAccountRequest {
    @NotBlank private String bankBin;
    @NotBlank private String bankCode;
    @NotBlank private String bankName;
    private String bankShortName;
    @NotBlank private String accountNumber;
    @NotBlank private String accountName;
    private Boolean isDefault = false;
}
