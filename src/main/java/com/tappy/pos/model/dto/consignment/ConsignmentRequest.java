package com.tappy.pos.model.dto.consignment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ConsignmentRequest {

    /** vendors.id of the publisher (optional — null = ad-hoc supplier). */
    private Long publisherId;

    @NotBlank
    private String publisherName;

    @NotNull
    private LocalDate placementDate;

    private String note;

    @NotEmpty
    @Valid
    private List<ConsignmentItemRequest> items;
}
