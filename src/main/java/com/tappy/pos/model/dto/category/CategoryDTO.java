package com.tappy.pos.model.dto.category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {

    private Long id;
    private String name;
    private Long parentId;
    private String parentName;
    private int childCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
