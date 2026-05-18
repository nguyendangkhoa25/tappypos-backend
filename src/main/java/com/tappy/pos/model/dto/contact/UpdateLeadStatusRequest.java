package com.tappy.pos.model.dto.contact;

import com.tappy.pos.model.enums.LeadStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateLeadStatusRequest {

    @NotNull
    private LeadStatus status;

    private String adminNote;
}
