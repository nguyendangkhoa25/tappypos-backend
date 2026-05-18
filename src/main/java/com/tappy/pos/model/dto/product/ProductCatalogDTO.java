package com.tappy.pos.model.dto.product;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCatalogDTO {
    private Long id;
    private String barcode;
    private String name;
    private String brand;
    private String categoryHint;
    private String unit;
    private String imageUrl;
    private String source;
    private LocalDateTime createdAt;
}
