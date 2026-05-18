package com.tappy.pos.model.dto.bank;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankAccountDTO {
    private Long id;
    private String bankBin;
    private String bankCode;
    private String bankName;
    private String bankShortName;
    private String accountNumber;
    private String accountName;
    private Boolean isDefault;
}
