package com.tappy.pos.model.dto.tenant;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaveVendorRequest {
    private String name;
    private String contactEmail;
    private String contactPhone;
    private String notes;
}
