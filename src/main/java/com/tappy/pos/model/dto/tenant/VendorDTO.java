package com.tappy.pos.model.dto.tenant;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorDTO {
    private Long id;
    private String name;
    private String contactEmail;
    private String contactPhone;
    private String notes;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
