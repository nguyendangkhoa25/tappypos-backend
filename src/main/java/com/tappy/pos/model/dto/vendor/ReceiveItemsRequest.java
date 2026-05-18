package com.tappy.pos.model.dto.vendor;

import lombok.Data;

import java.util.List;

@Data
public class ReceiveItemsRequest {
    private List<ItemReceive> items;
    private String notes;

    @Data
    public static class ItemReceive {
        private Long itemId;
        private Integer quantityReceived;
    }
}
