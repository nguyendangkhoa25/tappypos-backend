package com.tappy.pos.model.dto.contact;

import com.tappy.pos.model.enums.LeadStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ContactLeadDTO {
    private Long id;
    private String name;
    private String phone;
    private String shopType;
    private String note;
    private String source;
    private LeadStatus status;
    private String statusDisplayName;
    private String adminNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
